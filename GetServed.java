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

/**
 * Class for server logic. Starts the server, accepts clients, and tracks
 * information.
 */
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

    /**
     * Constructor: initializes fields and starts the timer for an idle server.
     * 
     * @param port     the port the server will run on
     * @param reporter a Reporter for the server to print messages
     * @throws IOException as needed, for main method to catch
     */
    public GetServed(int port, Reporter reporter) throws IOException {
        serverSocket = new ServerSocket(port);
        this.reporter = reporter;
        nickNameIdxMain = 0;
        currNicknames = new String[4];
        currSockets = new Socket[4];
        channels = new ArrayList<ChannelInfo>();
        helpMsg = "\n-\n" +
                "/connect <server-name> [port#]\n" + "Connect to named server (port# optional)\n\n" +
                "/nick <nickname>\n" + "Pick a nickname (should be unique among active users)\n\n" +
                "/list\n" + "List channels and number of users\n\n" +
                "/join <channel>\n" + "Join a channel, all text typed is sent to all users on the channel\n\n" +
                "/refresh\n" + "Updates screen with new messages if you are in a channel\n\n" +
                "/leave [<channel>]\n" + "Leave the current (or named) channel\n\n" +
                "/quit\n" + "Leave chat and disconnect from server\n\n" +
                "/help\n" + "Print out help message\n-\n";
        shutdownTimer = new Timer();
        idxLock = new Object();
        idleLock = new Object();
        startTimer();
    }

    /**
     * Starts a timer task to shut down the server if idle for 3 minutes.
     */
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

    /**
     * Removes relevant records of a client nickname, and maintains original data
     * structures
     * 
     * @param idx the index where the nickname to be removed is
     */
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

    /**
     * Determines if the nickname array will become empty after removing a nickname
     * 
     * @param array the array that is being analyzed for emptiness
     * @return true if the array is empty, false if not
     */
    private boolean emptyArray(String[] array) {
        for (String s : array) {
            if (s != null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determines if the clients array is full
     * 
     * @return true if the array has four nicknames and connections, false if not
     */
    private boolean full() {
        for (String n : currNicknames) {
            if (n == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Main logic for accepting clients and starting threads that maintain
     * connections with them.
     */
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
                    synchronized (currNicknames) {// check if four are in
                        if (full()) {
                            reporter.report("client rejected due to full pool", 1, "yellow");
                            client.close();
                            continue;
                        }
                    }
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
                        out.writeObject(new StringObject("default" + currNNIdx));
                        out.flush();
                        reporter.report("new client connection: default" + currNNIdx, 1, "yellow");

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

    /**
     * Determines if a client input is a command or not. Checks the beginning of the
     * string to find this out.
     * 
     * @param cmd the command
     * @return true if the input is a command, false if not
     */
    private static boolean cmd(String cmd) {
        String lCmd = cmd.toLowerCase();
        for (String c : cmds) {
            if (lCmd.startsWith(c)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Class that creates a connection with each client that connects to the server,
     * used in a multi-threading format.
     */
    private class ServerThread implements Runnable {
        private ObjectInputStream in;
        private ObjectOutputStream out;
        private String currNickname;
        private int nickNameIdx;
        private String channelName;

        /**
         * Constructor, initializes fields
         * 
         * @param in          the input stream for reading client objects
         * @param out         the output stream for sending client objects
         * @param nickNameIdx the index of the client's nickname in the GetServed class
         *                    array
         */
        private ServerThread(ObjectInputStream in, ObjectOutputStream out, int nickNameIdx) {
            this.in = in;
            this.out = out;
            this.nickNameIdx = nickNameIdx;
            synchronized (currNicknames) {
                this.currNickname = currNicknames[nickNameIdx];
            }
            this.channelName = "";
        }

        /**
         * Finds the ChannelInfo object of the current channelName
         * 
         * @return the corresponding ChannelInfo object, or null if not present
         */
        private ChannelInfo currChannel() {
            synchronized (channels) {
                for (ChannelInfo channel : channels) {
                    if (channel.getName().equals(channelName)) {
                        return channel;
                    }
                }
                return null;

            }
        }

        /**
         * Find the channel with the currNickName, and gets the unread messages for the
         * client from the channel.
         * 
         * @return the unread messages, or the blank string if they are not present
         */
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
        public void run() {// main protocol with client, checks commands and sends appropriate responses
            try {
                boolean open = true;
                while (open) {
                    String currCmd = "";
                    if (!channelName.equals("")) {
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
                    if (currCmd.isBlank() || currCmd.startsWith("/connect")) {
                        continue;
                    }
                    if (!channelName.equals("")) {// adds message to channel
                        if (!cmd(currCmd)) {
                            Date date = new Date();
                            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy h:mm:ss a");
                            String fD = sdf.format(date);
                            currChannel()
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
                        // above conditions are checking for correct command
                        String newNickname = currCmd.substring(6);
                        boolean unique = true;
                        // check if new nickname is unique among all users
                        synchronized (currNicknames) {
                            for (String n : currNicknames) {
                                if (n != null && n.equals(newNickname)) {// nickname already exists
                                    out.writeObject(new StringObject("the new nickname is not unique, try again"));
                                    out.flush();
                                    reporter.report(
                                            currNickname + " attempted to change nickname into a non-unique value",
                                            1, "red");
                                    unique = false;
                                    break;
                                }
                            }
                        }
                        if (unique) {
                            // at this point, change the nickname
                            synchronized (currNicknames) {
                                String oldNickname = currNicknames[nickNameIdx];
                                currNicknames[nickNameIdx] = newNickname;
                                currNickname = newNickname;
                                // change in channel
                                if (!channelName.equals("")) {
                                    currChannel().changeNickName(oldNickname, newNickname);
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
                            && channelName.equals("")) {
                        String possChannelName = currCmd.substring(6);
                        synchronized (channels) {
                            ChannelInfo channelToJoin = null;
                            if (!channels.isEmpty()) {
                                for (ChannelInfo channel : channels) { // makes sure the channel name is unique
                                    if (channel.getName().equals(possChannelName)) {
                                        channelToJoin = channel;
                                        break;
                                    }
                                }
                            }
                            String s1;
                            channelName = possChannelName;
                            if (channelToJoin != null) {// channel already exists, join it
                                channelToJoin.addMember(currNickname);
                                s1 = "joined existing channel " + channelToJoin.getName();
                                reporter.report(
                                        currNickname + " joined existing channel " + channelToJoin.getName(),
                                        1, "cyan");

                            } else { // create new channel
                                channelToJoin = new ChannelInfo(possChannelName, currNickname);
                                synchronized (channels) {
                                    channels.add(channelToJoin);
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

                    } else if ((currCmd.equals("/leave") && !channelName.equals(""))
                            || (currCmd.startsWith("/leave")
                                    && (currCmd.length() >= 8 && !currCmd.substring(7).trim().isEmpty()
                                            && currCmd.substring(6, 7).equals(" ")))
                                    && !channelName.equals("")) {
                        // get substring if it exists
                        String channelToLeaveNameFromCmd = "";
                        if (currCmd.length() >= 8) {
                            channelToLeaveNameFromCmd = currCmd.substring(7);
                        } else {
                            channelToLeaveNameFromCmd = channelName;
                        }
                        synchronized (channels) {
                            // makes sure if optional argument is used, it matches the channel they are in
                            if (!channelToLeaveNameFromCmd.equals(currChannel().getName())) {
                                out.writeObject(
                                        new StringObject(
                                                "Bad leave command, likely due to incorrect usage of the optional [<channel>] arg (not entering the channel name correctly). An easier command to use is to simply use \"/leave\", which will leave the current channel."));
                                out.flush();
                                reporter.report("sent /leave retry message to client " + currNickname, 1, "cyan");
                            } else {// actually leave the channel
                                // send all messages still not sent yet
                                String msgs = sendMessages();
                                currChannel().removeMember(currNickname);
                                out.writeObject(new StringObject(msgs + "\nleft channel " + currChannel().getName()));
                                out.flush();
                                reporter.report(currNickname + " left channel " + currChannel().getName(), 1, "black");
                                channelName = "";
                            }
                        } // end sync

                    } else if (currCmd.equals("/quit")) {
                        // leave entire server
                        String quitMsg = "";
                        // leave channel
                        if (!channelName.equals("")) {
                            ChannelInfo channelToLeave = currChannel();
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
                            channelName = "";
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

                    } else { // improper commands
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

    /**
     * Object for storing channel information
     */
    private class ChannelInfo {
        String name;
        HashMap<String, String> members;
        HashMap<String, ArrayList<String>> messages;
        List<String> channelColors;

        /**
         * Constructor: initializes objects and adds first member of the channel
         * 
         * @param name the name of the channel
         * @param firstMember the first client who joined this new channel
         */
        private ChannelInfo(String name, String firstMember) {
            this.name = name;
            members = new HashMap<>();
            messages = new HashMap<>();
            channelColors = TermColors.channelColors();
            addMember(firstMember);
        }

        /**
         * @return the channel name
         */
        private synchronized String getName() {
            return name;
        }

        /**
         * @return the list of member nicknames
         */
        private synchronized Set<String> getMembers() {
            return members.keySet();
        }

        /**
         * @param mem the nickname of the member to remove
         */
        private synchronized void removeMember(String mem) {
            members.remove(mem);
        }

        /**
         * Adds a new member to the channel, and gives them a color for their messages in the channel
         * 
         * @param newMember the nickname of the new member
         */
        private synchronized void addMember(String newMember) {
            String color = channelColors.remove(0);
            members.put(newMember, color);
            channelColors.add(color);
        }

        /**
         * Adds a message for each channel member to read
         * 
         * @param message the message to add for each member
         */
        private synchronized void addMessage(String message) {
            if (!message.isBlank()) {
                for (Entry<String, String> member : members.entrySet()) {
                    messages.computeIfAbsent(member.getKey(), messages -> new ArrayList<String>())
                            .add(member.getValue() + "|" + message);
                }
            }
        }

        /**
         * Changes the nickname information in the  channel
         * 
         * @param oldN the current nickname of someone in the channel
         * @param newN the new nickname that will replace the current one
         */
        private synchronized void changeNickName(String oldN, String newN) {
            String color = members.get(oldN);
            String msgs = sendMessages(oldN);
            System.out.println("msgs: " + msgs);
            members.remove(oldN);
            members.put(newN, color);
            addMessage(msgs);
        }

        /**
         * Removes and sends messages to a client member
         * 
         * @param member the client who will receive their messages
         * @return the String representation of the messages
         */
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
