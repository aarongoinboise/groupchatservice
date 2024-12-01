import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GetServed {
    private static final ExecutorService pool = Executors.newFixedThreadPool(4);
    private ServerSocket serverSocket;
    private Reporter reporter;
    private int nickNameIdxMain;
    private String[] currNicknames;
    private Socket[] currSockets;
    private ArrayList<ChannelInfo> channels;
    private String helpMsg;
    private static String[] cmds = { "/connect", "/nick", "/list", "/join", "/leave", "/quit", "/help" };
    private Timer shutdownTimer;
    private boolean idle;

    public GetServed(int port, Reporter reporter) throws IOException {
        serverSocket = new ServerSocket(port);
        this.reporter = reporter;
        nickNameIdxMain = 0;
        currNicknames = new String[4];
        currSockets = new Socket[4];
        channels = new ArrayList<ChannelInfo>();
        helpMsg = "Command\tDescription\n" +
                "/connect <server-name> [port#]\tConnect to named server (port# optional)\n" +
                "/nick <nickname>\tPick a nickname (should be unique among active users)\n" +
                "/list\tList channels and number of users\n" +
                "/join <channel>\tJoin a channel, all text typed is sent to all users on the channel\n" +
                "/leave [<channel>]\tLeave the current (or named) channel\n" +
                "/quit\tLeave chat and disconnect from server\n" +
                "/help\tPrint out help message";
        shutdownTimer = new Timer();
        startTimer();
    }

    public void startTimer() {
        reporter.report("starting shutdown timer", 1, "green");
        shutdownTimer.cancel();
        shutdownTimer = new Timer();

        TimerTask task = new TimerTask() {
            public void run() {
                if (idle) {
                    try {
                        serverSocket.close();
                    } catch (IOException e) {
                        reporter.report("couldn't close server socket", 0, "red");
                    }
                }
            }
        };

        shutdownTimer.schedule(task, 180000);
    }

    public synchronized void removeNickname(int idx) {
        currNicknames[idx] = null;
        String[] newArray = new String[4];
        Socket[] newSocks = new Socket[4];
        int newIdx = 0;
        for (int i = 0; i < currNicknames.length; i++) {
            if (currNicknames[i] != null) {
                newArray[newIdx] = currNicknames[i];
                newSocks[newIdx] = currSockets[i];
                newIdx++;
            }
        }
        if (emptyArray(newArray)) {
            idle = true;
            startTimer();
        }
        nickNameIdxMain = newIdx;
        currNicknames = newArray;
        currSockets = newSocks;
    }

    private boolean emptyArray(String[] array) {
        for (String s : array) {
            if (s != null) {
                return false;
            }
        }
        return true;
    }

    public void youGotServed() {
        reporter.report("Server " + serverSocket.getInetAddress() + " up on port " + serverSocket.getLocalPort()
                + " waiting for clients...", 1, "blue");
        while (true) {
            try {
                Socket client = serverSocket.accept();
                client.setSoTimeout(5000);
                ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(client.getInputStream());
                currNicknames[nickNameIdxMain] = "default" + nickNameIdxMain;
                currSockets[nickNameIdxMain] = client;
                idle = false;
                int currNNIdx = nickNameIdxMain;
                if (nickNameIdxMain == 3) {
                    nickNameIdxMain = 0;
                } else {
                    nickNameIdxMain++;
                }
                out.writeObject(new StringObject("default" + nickNameIdxMain));
                out.flush();
                reporter.report("new client connection: default" + nickNameIdxMain, 1, "yellow");
                ServerConnection2 serverConnection = new ServerConnection2(in, out, currNNIdx);
                pool.execute(serverConnection);
            } catch (IOException e) {
                synchronized (currNicknames) {
                    synchronized (currSockets) {
                        for (int i = 0; i < currSockets.length; i++) {
                            if (currSockets[i].isClosed()) {
                                reporter.report("client " + currNicknames[i] + " disconnected", 0, "black");
                                removeNickname(i);
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean cmd(String cmd) {
        String lCmd = cmd.toLowerCase();
        for (String c : cmds) {
            if (lCmd.startsWith(c)) {
                return true;
            }
        }
        return false;
    }

    // ServerConnection2 implements Runnable instead of extending Thread
    private class ServerConnection2 implements Runnable {
        private ObjectInputStream in;
        private ObjectOutputStream out;
        private String currNickname;
        private int nickNameIdx;
        private boolean inChannel;

        private ServerConnection2(ObjectInputStream in, ObjectOutputStream out, int nickNameIdx) {
            this.in = in;
            this.out = out;
            this.nickNameIdx = nickNameIdx;
            this.currNickname = currNicknames[nickNameIdx];
            this.inChannel = false;
        }

        private synchronized String sendMessages() {
            ChannelInfo currChannel = null;
            for (ChannelInfo channel : channels) {
                if (channel.members.keySet().contains(currNickname)) {
                    currChannel = channel;
                    break;
                }
            }
            if (currChannel != null) {
                return currChannel.sendMessages(currNickname);
            }
            return "";
        }

        private synchronized ChannelInfo currChannel(String channelName) {
            ChannelInfo channelToPick = null;
            for (ChannelInfo channel : channels) {
                if (channel.members.keySet().contains(currNickname)) {
                    // check that it equals channelToLeave if needed
                    if (!channelName.equals("")
                            && channelName.equals(channel.name)) {// correct command
                        channelToPick = channel;
                    } else if (channelName.equals("")) {
                        channelToPick = channel;
                    }
                    break;
                }
            }
            return channelToPick;
        }

        @Override
        public void run() {
            try {
                boolean open = true;
                while (open) {
                    String currCmd = "";
                    if (inChannel) {
                        // check if message is to be sent or and check the inputs if any
                        String cMs = currChannel("").sendMessages(helpMsg);
                        out.writeObject(new StringObject(cMs));
                        out.flush();
                    } else {// just send blank message
                        out.writeObject(new StringObject(""));
                        out.flush();
                    }
                    try {
                        currCmd = ((StringObject) in.readObject()).toString();
                    } catch (SocketTimeoutException e) {
                        reporter.report("Input timeout for " + currNickname, 0, "white");
                        continue;
                    }
                    if (inChannel) {
                        if (!cmd(currCmd)) {
                            Date date = new Date();
                            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy h:mm:ss a");
                            String fD = sdf.format(date);
                            currChannel("").addMessage("FROM: " + currNickname + " | " + fD + " | " + currCmd);
                            reporter.report("client " + currNickname + " sent message to channel", 1, "purple");
                            continue;
                        }
                    }

                    reporter.report("client " + currNickname + " sent command " + currCmd, 1, "purple");

                    if (currCmd.equals("/help")) {
                        out.writeObject(new StringObject(helpMsg));
                        out.flush();
                        reporter.report("sent help message to client " + currNickname, 1, "blue");

                    } else if (currCmd.startsWith("/nick") && currCmd.length() >= 7
                            && !currCmd.substring(6).trim().isEmpty() && currCmd.substring(5, 6).equals(" ")) {
                        String newNickname = currCmd.substring(6);
                        boolean unique = true;
                        // check if new nickname is unique among all users
                        for (String n : currNicknames) {
                            if (n.equals(newNickname)) {
                                out.writeObject(new StringObject("the new nickname is not unique, try again"));
                                out.flush();
                                reporter.report(
                                        currNickname + " attempted to change nickname into a non-unique value",
                                        1, "red");
                                unique = false;
                                break;
                            }
                        }
                        if (unique) {
                            // at this point, change the nickname
                            synchronized (currNicknames) {
                                String oldNickname = currNicknames[nickNameIdx];
                                currNicknames[nickNameIdx] = newNickname;
                                // change in channel
                                if (inChannel) {
                                    ChannelInfo chan = currChannel("");
                                    chan.changeNickName(oldNickname, newNickname);
                                }

                                out.writeObject(new StringObject("your new nickname is " + newNickname));
                                out.flush();
                                reporter.report(oldNickname + " changed the nickname to " + newNickname,
                                        1, "yellow");
                            }
                        }

                    } else if (currCmd.equals("/list")) {
                        String cInfo = "/list results:\n";
                        synchronized (channels) {
                            if (channels.isEmpty()) {
                                cInfo += "No channels exist on this server!";

                            } else {
                                for (ChannelInfo channel : channels) {
                                    cInfo += "-\n";
                                    cInfo += "Channel name with members below: " + channel.name + "\n";
                                    for (String member : channel.members.keySet()) {
                                        cInfo += member + "\n";
                                    }
                                    cInfo += "-\n";
                                }
                            }
                        }
                        out.writeObject(new StringObject(cInfo));
                        out.flush();
                    } else if (currCmd.startsWith("/join") && currCmd.length() >= 7
                            && !currCmd.substring(6).trim().isEmpty() && currCmd.substring(5, 6).equals(" ")
                            && !inChannel) {
                        String possChannelName = currCmd.substring(6);
                        synchronized (channels) {
                            ChannelInfo channelToJoin = null;
                            if (!channels.isEmpty()) {
                                for (ChannelInfo channel : channels) {
                                    if (channel.name.equals(possChannelName)) {
                                        channelToJoin = channel;
                                        break;
                                    }
                                }
                            }
                            String s1;
                            if (channelToJoin != null) {
                                channelToJoin.addMember(currNickname);
                                s1 = "joined existing channel " + channelToJoin.name;
                                reporter.report(
                                        currNickname + " joined existing channel " + channelToJoin.name,
                                        1, "cyan");

                            } else { // create new channel
                                channelToJoin = new ChannelInfo(possChannelName, currNickname);
                                channels.add(channelToJoin);
                                s1 = "created a new channel called " + channelToJoin.name;
                                reporter.report(
                                        currNickname + " created a new channel called " + channelToJoin.name,
                                        1, "cyan");
                            }
                            s1 += ". Chat messages will now display. Any non-command message you enter will now display in the channel. Commands will still work in addition to chat messages.";
                            out.writeObject(new StringObject(s1));
                            out.flush();
                            inChannel = true;
                        }

                    } else if ((currCmd.startsWith("/leave")
                            && (currCmd.length() >= 8 && !currCmd.substring(7).trim().isEmpty()
                                    && currCmd.substring(6, 7).equals(" ")))
                            && inChannel) {
                        // get substring if it exists
                        String channelToLeaveNameFromCmd = "";
                        if (currCmd.length() >= 8) {
                            channelToLeaveNameFromCmd = currCmd.substring(7);
                        }
                        synchronized (channels) {
                            ChannelInfo channelToLeave = currChannel(channelToLeaveNameFromCmd);
                            if (channelToLeave == null) {
                                out.writeObject(
                                        new StringObject(
                                                "Bad leave command, likely due to incorrect usage of the optional [<channel>] arg (not entering the channel name correctly). An easier command to use is to simply use \"/leave\", which will leave the current channel."));
                                out.flush();
                                reporter.report("sent /leave retry message to client " + currNickname, 1, "cyan");
                            } else {// actually leave the channel
                                // send all messages still not sent yet
                                String msgs = sendMessages();
                                channelToLeave.members.remove(currNickname);
                                out.writeObject(new StringObject(msgs + "\nleft channel " + channelToLeave.name));
                                out.flush();
                                reporter.report(currNickname + " left channel " + channelToLeave.name, 1, "black");
                                inChannel = false;
                            }
                        } // end sync

                    } else if (currCmd.equals("/quit")) {
                        // leave entire server
                        String quitMsg = "";
                        // leave channel
                        if (inChannel) {
                            ChannelInfo channelToLeave = currChannel("");
                            if (channelToLeave == null) {
                                quitMsg += "Internal error, while quitting your channel was not found.\n";
                                reporter.report(currNickname + " could not leave channel while quitting", 0, "red");
                            } else {// actually leave the channel
                                // send all messages still not sent yet
                                String msgs = sendMessages();
                                channelToLeave.members.remove(currNickname);
                                quitMsg += msgs + "\nleft channel " + channelToLeave.name;
                                reporter.report(currNickname + " left channel " + channelToLeave.name, 1, "black");
                            }
                        }
                        out.writeObject(new StringObject(
                                quitMsg + "\nLeaving server " + serverSocket.getInetAddress() + "..."));
                        out.flush();
                        removeNickname(nickNameIdx);
                        in.close();
                        out.close();
                        currSockets[nickNameIdx].close();
                        open = false;

                    } else {
                        out.writeObject(
                                new StringObject(
                                        "Bad command due to improper syntax (i.e. spacing) or usage. Examples include joining a channel while in another one, or leaving a channel that you aren't currently in. Try again"));
                        out.flush();
                        reporter.report("sent retry message to client " + currNickname, 1, "cyan");
                    } // end else
                } // end while

            } catch (IOException e) {
                reporter.report("client " + currNickname + " disconnected", 0, "red");
            } catch (Exception e) {
                reporter.report("exception " + e.toString() + " occurred", 0, "red");
            }
        }
    }

    private class ChannelInfo {
        String name;
        HashMap<String, String> members;
        HashMap<String, ArrayList<String>> messages;
        List<String> channelColors;

        private ChannelInfo(String name, String firstMember) {
            this.name = name;
            members = new HashMap<>();
            addMember(firstMember);
            messages = new HashMap<>();
            channelColors = TermColors.channelColors();
        }

        private void addMember(String newMember) {
            String color = channelColors.remove(0);
            members.put(newMember, color);
            channelColors.add(color);
        }

        private synchronized void addMessage(String message) {
            for (Entry<String, String> member : members.entrySet()) {
                messages.computeIfAbsent(member.getKey(), messages -> new ArrayList<String>()).add(member.getValue() + message + TermColors.reset);
            }
        }

        private synchronized void changeNickName(String oldN, String newN) {
            members.remove(oldN);
            addMember(newN);
            ArrayList<String> m = messages.remove(oldN);
            if (m != null && !m.isEmpty()) {
                messages.computeIfAbsent(newN, messages -> new ArrayList<String>()).add(String.join("\n", m));
            }
        }

        private synchronized String sendMessages(String member) {
            if (!messages.containsKey(member)) {
                return "";
            }
            ArrayList<String> memberMessages = messages.remove(member);
            if (memberMessages == null) {
                return "";
            }
            return String.join("\n", memberMessages);
        }
    }
}
