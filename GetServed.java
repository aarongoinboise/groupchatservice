import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
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
    private static String[] cmds = { "/connect", "/refresh", "/nick", "/list", "/join", "/leave", "/quit",
            "/help" };
    private Timer shutdownTimer;
    private boolean idle;
    private Object idxLock;
    private Object idleLock;

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
                "/refresh\tUpdates screen with new messages if you are in a channel\n" +
                "/leave [<channel>]\tLeave the current (or named) channel\n" +
                "/quit\tLeave chat and disconnect from server\n" +
                "/help\tPrint out help message";
        shutdownTimer = new Timer();
        idxLock = new Object();
        idleLock = new Object();
        startTimer();
    }

    public void startTimer() {
        reporter.report("starting shutdown timer", 1, "green");
        shutdownTimer.cancel();
        shutdownTimer = new Timer();
        TimerTask task = new TimerTask() {
            public void run() {
                synchronized (idleLock) {
                    if (idle) {
                        try {
                            synchronized (serverSocket) {
                                serverSocket.close();
                            }
                        } catch (IOException e) {
                            reporter.report("couldn't close server socket", 0, "red");
                        }
                    }
                }
            }
        };
        shutdownTimer.schedule(task, 180000);
    }

    public void removeNickname(int idx) {
        synchronized (currNicknames) {
            synchronized (currSockets) {
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
                    synchronized (idleLock) {
                        idle = true;
                    }
                    startTimer();
                }
                synchronized (idxLock) {
                    nickNameIdxMain = newIdx;
                }
                currNicknames = newArray;
                currSockets = newSocks;
            }
        }
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
        synchronized (serverSocket) {
            reporter.report("Server " + serverSocket.getInetAddress() + " up on port " + serverSocket.getLocalPort()
                    + " waiting for clients...", 1, "blue");
        }
        while (true) {
            try {
                synchronized (serverSocket) {
                    Socket client = serverSocket.accept();
                    ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
                    ObjectInputStream in = new ObjectInputStream(client.getInputStream());
                    synchronized (currNicknames) {
                        synchronized (currSockets) {
                            currNicknames[nickNameIdxMain] = "default" + nickNameIdxMain;
                            currSockets[nickNameIdxMain] = client;
                        }
                    }
                    synchronized (idleLock) {
                        idle = false;
                    }
                    synchronized (idxLock) {
                        int currNNIdx = nickNameIdxMain;
                        if (nickNameIdxMain == 3) {
                            nickNameIdxMain = 0;
                        } else {
                            nickNameIdxMain++;
                        }
                        out.writeObject(new StringObject("default" + nickNameIdxMain));
                        out.flush();
                        reporter.report("new client connection: default" + nickNameIdxMain, 1, "yellow");

                        ServerThread serverConnection = new ServerThread(in, out, currNNIdx);
                        pool.execute(serverConnection);
                    }
                }
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
    private class ServerThread implements Runnable {
        private ObjectInputStream in;
        private ObjectOutputStream out;
        private String currNickname;
        private int nickNameIdx;
        private ChannelInfo currChannel;

        private ServerThread(ObjectInputStream in, ObjectOutputStream out, int nickNameIdx) {
            this.in = in;
            this.out = out;
            this.nickNameIdx = nickNameIdx;
            synchronized (currNicknames) {
                this.currNickname = currNicknames[nickNameIdx];
            }
            this.currChannel = null;
        }

        private String sendMessages() {
            synchronized (channels) {
                ChannelInfo currChannel = null;
                for (ChannelInfo channel : channels) {
                    if (channel.getMembers().contains(currNickname)) {
                        currChannel = channel;
                        break;
                    }
                }
                if (currChannel != null) {
                    return currChannel.sendMessages(currNickname);
                }
            }
            return "";
        }

        @Override
        public void run() {
            try {
                boolean open = true;
                while (open) {
                    String currCmd = "";
                    if (currChannel != null) {
                        // check if message is to be sent or and check the inputs if any
                        String cMs = sendMessages();
                        if (cMs.isBlank()) {
                            out.writeObject(new StringObject("No new messages..."));
                            out.flush();
                        } else {
                            out.writeObject(new StringObject(cMs));
                            out.flush();
                        }
                    } else {// just send blank message
                        out.writeObject(new StringObject(""));
                        out.flush();
                    }
                    currCmd = ((StringObject) in.readObject()).toString();
                    if (currChannel != null) {
                        if (currCmd.isBlank()) {
                            out.writeObject(
                                    new StringObject("no blank messages allowed, because they server no purpose."));
                            out.flush();
                            continue;
                        }
                        if (!cmd(currCmd)) {
                            System.out.println(currCmd);
                            Date date = new Date();
                            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy h:mm:ss a");
                            String fD = sdf.format(date);
                            currChannel
                                    .addMessage("FROM: " + currNickname + " | DATE: " + fD + " | MESSAGE: " + currCmd);
                            reporter.report("client " + currNickname + " sent message to channel ", 1, "purple");
                            out.writeObject(new StringObject("added message to channel"));
                            out.flush();
                            continue;
                        } else if (currCmd.equals("/refresh")) {
                            out.writeObject(new StringObject("refreshing channel messages..."));
                            out.flush();
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
                                if (currChannel != null) {
                                    currChannel.changeNickName(oldNickname, newNickname);
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
                                    cInfo += "Channel name with members below: " + channel.getName() + "\n";
                                    for (String member : channel.getMembers()) {
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
                            && currChannel == null) {
                        String possChannelName = currCmd.substring(6);
                        synchronized (channels) {
                            ChannelInfo channelToJoin = null;
                            if (!channels.isEmpty()) {
                                for (ChannelInfo channel : channels) {
                                    if (channel.getName().equals(possChannelName)) {
                                        channelToJoin = channel;
                                        break;
                                    }
                                }
                            }
                            String s1;
                            if (channelToJoin != null) {
                                channelToJoin.addMember(currNickname);
                                s1 = "joined existing channel " + channelToJoin.getName();
                                reporter.report(
                                        currNickname + " joined existing channel " + channelToJoin.getName(),
                                        1, "cyan");

                            } else { // create new channel
                                channelToJoin = new ChannelInfo(possChannelName, currNickname);
                                synchronized (channels) {
                                    channels.add(channelToJoin);
                                    currChannel = channelToJoin;
                                }
                                s1 = "created a new channel called " + channelToJoin.getName();
                                reporter.report(
                                        currNickname + " created a new channel called " + channelToJoin.getName(),
                                        1, "cyan");
                            }
                            s1 += ". Chat messages will now display. Any non-command message you enter will now display in the channel. Commands will still work in addition to chat messages.";
                            out.writeObject(new StringObject(s1));
                            out.flush();
                        } // end sync

                    } else if ((currCmd.startsWith("/leave")
                            && (currCmd.length() >= 8 && !currCmd.substring(7).trim().isEmpty()
                                    && currCmd.substring(6, 7).equals(" ")))
                            && currChannel != null) {
                        // get substring if it exists
                        String channelToLeaveNameFromCmd = "";
                        if (currCmd.length() >= 8) {
                            channelToLeaveNameFromCmd = currCmd.substring(7);
                        }
                        synchronized (channels) {
                            if (!channelToLeaveNameFromCmd.equals(currChannel.getName())) {
                                out.writeObject(
                                        new StringObject(
                                                "Bad leave command, likely due to incorrect usage of the optional [<channel>] arg (not entering the channel name correctly). An easier command to use is to simply use \"/leave\", which will leave the current channel."));
                                out.flush();
                                reporter.report("sent /leave retry message to client " + currNickname, 1, "cyan");
                            } else {// actually leave the channel
                                // send all messages still not sent yet
                                String msgs = sendMessages();
                                currChannel.removeMember(currNickname);
                                out.writeObject(new StringObject(msgs + "\nleft channel " + currChannel.getName()));
                                out.flush();
                                reporter.report(currNickname + " left channel " + currChannel.getName(), 1, "black");
                                currChannel = null;
                            }
                        } // end sync

                    } else if (currCmd.equals("/quit")) {
                        // leave entire server
                        String quitMsg = "";
                        // leave channel
                        if (currChannel != null) {
                            ChannelInfo channelToLeave = currChannel;
                            if (channelToLeave == null) {
                                quitMsg += "Internal error, while quitting your channel was not found.\n";
                                reporter.report(currNickname + " could not leave channel while quitting", 0, "red");
                            } else {// actually leave the channel
                                // send all messages still not sent yet
                                String msgs = sendMessages();
                                channelToLeave.removeMember(currNickname);
                                quitMsg += msgs + "\nleft channel " + channelToLeave.getName();
                                reporter.report(currNickname + " left channel " + channelToLeave.getName(), 1, "black");
                            }
                            currChannel = null;
                        }
                        synchronized (serverSocket) {
                            out.writeObject(new StringObject(
                                    quitMsg + "\nLeaving server " + serverSocket.getInetAddress() + "..."));
                        }
                        out.flush();
                        removeNickname(nickNameIdx);
                        in.close();
                        out.close();
                        synchronized (currSockets) {
                            currSockets[nickNameIdx].close();
                        }
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
                reporter.report("exception occurred: " + e.toString(), 0, "red");
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
            messages = new HashMap<>();
            channelColors = TermColors.channelColors();
            addMember(firstMember);
        }

        private synchronized String getName() {
            return name;
        }

        private synchronized Set<String> getMembers() {
            return members.keySet();
        }

        private synchronized void removeMember(String mem) {
            members.remove(mem);
        }

        private synchronized void addMember(String newMember) {
            String color = channelColors.remove(0);
            members.put(newMember, color);
            channelColors.add(color);
        }

        private synchronized void addMessage(String message) {
            for (Entry<String, String> member : members.entrySet()) {
                messages.computeIfAbsent(member.getKey(), messages -> new ArrayList<String>())
                        .add(member.getValue() + "|" + message);
            }
        }

        private synchronized void changeNickName(String oldN, String newN) {
            removeMember(oldN);
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
