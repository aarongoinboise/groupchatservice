import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The multi-threaded server who connects to clients. Stores some information
 * about client connections as well as channels, which clients can sign up for.
 */
public class Server extends Thread {
    public static final int SHUTDOWN_DELAY = 180000;//measured in milliseconds
    public static final int NUM_THREADS = 4;
	public static final ExecutorService pool = Executors.newFixedThreadPool(NUM_THREADS);
    private ServerSocket ss;
    private ArrayList<Channel> channels;
    private ArrayList<ClientConnection> clients;
    private int nicknameIdentity; // used to create unique, default nicknames
    private Timer shutdownTimer;
    private Timer channelDeathCount;
    private final String[] COMMANDS = { "COMMAND", "/connect <server-name>", "/nick <nickname>", "/list",
            "/join <channel>", "/leave [<channel>]", "/quit", "/help", "/stats" };
    private boolean idle;

    /**
     * constructor for ChatServer
     * 
     * @throws IOException
     */
    public Server(int port) throws IOException {
        ss = new ServerSocket(port);
        channels = new ArrayList<Channel>();
        clients = new ArrayList<ClientConnection>();
        nicknameIdentity = 1;
        idle = true;
        shutdownTimer = new Timer("shutdown timer");
        channelDeathCount = new Timer("channel death count");
        startTimer();
    }

    /**
     * Used when a client requires a default nickname. Also, increments
     * nicknameIdentity.
     * 
     * @param cC
     * @throws IOException
     */
    public synchronized void sendDefaultNickname(ClientConnection cC) throws IOException {
        cC.setNickName("nickname" + nicknameIdentity++);
    }

    /**
     * send the provided message to the specified client
     * 
     * @param cC
     * @param message
     * @throws IOException if there is an issue connecting to the client
     */
    public void writeMessage(ClientConnection cC, ChatMessage message) throws IOException {
        cC.writeMessage(message);
    }

    /**
     * reads a message from this client, blocks until there is a message
     * 
     * @param
     * @return message from the client or null
     * @throws ClassNotFoundException if the client did not sent a ChatMessage object
     * @throws IOException            if there is an issue connecting to the client
     */
    public ChatMessage readMessage(ClientConnection cC) throws ClassNotFoundException, IOException {
        return (ChatMessage) cC.in.readObject();
    }

    /**
     * Converts an existing channel name to a Channel object.
     * 
     * @param channelName
     * @return the existing channel, or null if the channel doesn't exist
     */
    public Channel channelNameToChannel(String channelName) {
        for (Channel c : channels) {
            if (c.getName().equals(channelName)) {
                return c;
            }
        }
        return null;
    }

    /**
     * Sends a shutdown message with stats to all connected clients. Used when server is shutdown (ctrl-c).
     * 
     * @throws IOException when message cannot be written
     */
    public void sendStatsAndShutdown() throws IOException {
        for (ClientConnection c : clients) {
            writeMessage(c, new ChatMessage("server", "Server was shutdown..." + getServerStats()));
            // let client handle it's own shutdown
        }
    }

    /**
     * Checks if a String is a valid command
     * 
     * @param cmd The String which is being checked
     * @return true if cmd is a command, false if not
     */
    private boolean inCmds(String cmd) {
        for (int i = 1; i < COMMANDS.length; i++) {
            if (COMMANDS[i].split(" ")[0].equals(cmd)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a nickname is unique among all active users
     * 
     * @param nickname the nickname
     * @return true if it is unique, false if not
     */
    private boolean uniqueNickname(String nickname) {
        for (ClientConnection client : clients) {
            if (!client.nicknameIsNull()) {
                if (client.getNickname().equals(nickname)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * @return a string with some information about the current state of the server
     */
    public String getServerStats(){
        StringBuilder statsString = new StringBuilder();

        statsString.append("\n" + "_".repeat(25 + 25) + "\n");
        statsString.append(String.format("| %-25s|%-25s\n", "number of channels", channels.size()));
        statsString.append("_".repeat(25 + 25) + "\n");
        statsString.append(String.format("| %-25s|%-25s\n", "number of users", clients.size()));
        statsString.append("_".repeat(25 + 25) + "\n");

        return statsString.toString();
    }

    /**
     * The main server method that accepts connections and starts off a new thread
     * to handle each accepted connection.
     * 
     * @throws InterruptedException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void runServer() throws InterruptedException, IOException, ClassNotFoundException {
        while (true) {
                Socket client = ss.accept();
                ClientConnection cC = new ClientConnection(
                        new ObjectInputStream(client.getInputStream()),
                        new ObjectOutputStream(client.getOutputStream()),
                        null);

                clients.add(cC);
                idle = false;
                ChatServer.report("idle is now false", 1);

                ServerConnection serverConnection = new ServerConnection(cC);
                pool.execute(serverConnection);
            }
        
    }

    /**
     * begins a timer for SHUTDOWN_DELAY milliseconds, after which the server is shut down if idle is true
     * additionally, cancels the timer from any previous calls to this method
     */
    public void startTimer() {
        ChatServer.report("starting shutdown timer, idle status: " + idle, 1);

        shutdownTimer.cancel();
        shutdownTimer = new Timer("shutdown timer");

        TimerTask task = new TimerTask() {
            public void run(){
                if (idle) {
                    try {
                        ss.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        shutdownTimer.schedule(task, SHUTDOWN_DELAY);
    }

    /**
     * begins a timer for SHUTDOWN_DELAY milliseconds, after which the channel is 
     * removed if it doesn't have any users additionally, cancels the timer from any 
     * previous calls to this method
     */
    public void startChannelDeathCount() {
        ChatServer.report("starting channel death count", 1);

        channelDeathCount.cancel();
        channelDeathCount = new Timer("channel death count");

        TimerTask task = new TimerTask() {
            public void run() {
                Iterator<Channel> iterator = channels.iterator();
                while (iterator.hasNext()) {
                    Channel channel = iterator.next();
                    if (channel.getNumUsers() == 0) {
                        ChatServer.report("removed channel " + channel.getName(), 1);
                        iterator.remove();
                    }
                }
            }
        };
    
        channelDeathCount.schedule(task, SHUTDOWN_DELAY);
    }

    /**
     * Establishes each connection with client
     */
    private class ServerConnection extends Thread {
        private StringBuilder helpMsg;
        private ClientConnection cC;
        private String channelName;
        private Channel channel;

        ServerConnection(ClientConnection cC) {
            this.helpMsg = new StringBuilder();
            this.cC = cC;
            this.channelName = null;
            this.channel = null;
            setPriority(NORM_PRIORITY - 1); // maybe not needed
            createHelpMsg();
        }

        /**
         * Initializes the help message that gets displayed to clients
         */
        private void createHelpMsg() {
            /* Create help message */
            String[] descriptions = {
                    "DESCRIPTION",
                    "Connect to named server",
                    "Pick a nickname (should be unique among active users)",
                    "List channels and number of users",
                    "Join a channel, all text typed is sent to all users on the channel",
                    "Leave the current (or named) channel",
                    "Leave chat and disconnect from server",
                    "Print out help message",
                    "Ask server for some stats"
            };
            helpMsg.append("\n" + "_".repeat(69 + 25) + "\n");
            for (int i = 0; i < 9; i++) {
                helpMsg.append(String.format("| %-25s|%-66s\n", COMMANDS[i], descriptions[i]));
                helpMsg.append("_".repeat(69 + 25) + "\n");
            }
        }

        /**
         * Returns existing channel with name or creates a new one
         * 
         * @param channelName
         * @return a new channel or existing one based on the name
         */
        private Channel currChannel(String channelName) {
            Channel channel = channelNameToChannel(channelName);
            if (channel == null) {
                channel = new Channel(channelName);
                channels.add(channel);
                return channel;

            } else {
                return channel;
            }
        }
        
        /**
         * Client leaves the channel they are in
         * Starts the timer to kill channel if there are no users
         * 
         * @throws IOException if channel cannot be left
         */
        private void leaveChannel() throws IOException {
            if (channel != null) {
                channel.removeClient(cC);
                writeMessage(cC, new ChatMessage(
                        "server",
                        "You left channel " + channelName));
                if (channel.getNumUsers() == 0) {
                    startChannelDeathCount();
                }
            }
            channelName = null;
            channel = null;
        }

        /**
         * Removes client from list. Also sets idle to true if no clients are in list.
         */
        private void removeClientFromList() {
            clients.remove(cC);
            if (clients.size() == 0) {
                idle = true;
                startTimer();
                ChatServer.report("idle is now true", 1);
            }
        }

        /**
         * Checks if leave command is valid
         * 
         * @param cmd the commmand
         * @return true if command is valid, false if not
         */
        private boolean leaveOptionalArgSpace(String cmd) {
            if (cmd.length() > 7) {
                if (cmd.charAt(6) == ' ') {
                    return true;
                }
                return false;
    
            } else {
                if (cmd.equals("/leave")) {
                    return true;
                }
                return false;
            }
        }

        @Override
        public void run() {
            try {
                while (cC.isOpen()) {
                    // check commands
                    String currCmd = (String) readMessage(cC).getMessage();
                    ChatServer.report("recieved message from client: " + cC.getNickname(), 1);
                    if (currCmd.equals("/help")) {
                        ChatServer.report("sending help message to client: " + cC.getNickname(), 1);
                        writeMessage(cC, new ChatMessage("server", helpMsg.toString()));
                    }

                    else if (currCmd.equals("/quit")) {
                        ChatServer.report("client: " + cC.getNickname() + " disconnected", 1);
                        writeMessage(cC, new ChatMessage("server", "Goodbye!"));
                        leaveChannel();
                        removeClientFromList();
                        cC.closeConnection();
                    }

                    /* Note: valid /nick commands need a space between /nick and the nickname */
                    else if (currCmd.startsWith("/nick") && currCmd.length() > 6 && currCmd.charAt(5) == ' ') {
                        String nickname = currCmd.substring(6).trim();

                        if (nickname.trim().isEmpty() || inCmds(nickname) || nickname.toLowerCase().contains("server".toLowerCase())) {
                            ChatServer.report("client: " + cC.getNickname() + " failed to change nickname due to invalid nickname", 1);
                            writeMessage(cC, new ChatMessage("server",
                                    "Pick a different, non-blank nickname with that doesn't have a command or the word server."));
                        } else if (!uniqueNickname(nickname)) {
                            ChatServer.report("client: " + cC.getNickname() + " failed to change nickname due to non-unique nickname", 1);
                            writeMessage(cC, new ChatMessage("server",
                                    "Pick a different nickname, that one isn't unique."));
                        } else {
                            ChatServer.report("client: " + cC.getNickname() + " changed nickname to: " + nickname, 1);
                            cC.setNickName(nickname);
                            writeMessage(cC, new ChatMessage("server",
                            "Your nickname is now " + nickname));
                        }
                    }

                    /* Note: valid /join commands need a space between /join and the channel name */
                    else if (currCmd.startsWith("/join") && currCmd.length() > 6 && currCmd.charAt(5) == ' ') {
                        if (channel != null) {// cannot join channel if you are already in one
							ChatServer.report("client: " + cC.getNickname() + " failed to join channel due to already being in a channel", 1);
                            writeMessage(cC, new ChatMessage(
                                    "server",
                                    "You are already connected to " + channelName
                                            + "! Disconnect if you would like to rejoin or join a different channel."));

                        } else {
                            /* Join channel */

                            /* Check channel name */
                            channelName = currCmd.substring(6, currCmd.length());
                            /* Add to server channel list if channel doesn't exist and set curr channel */
                            channel = currChannel(channelName);

                            /* Check nickname */
                            if (cC.nicknameIsNull()) {
                                /* Assign default nickname */
                                sendDefaultNickname(cC);
                                writeMessage(cC, new ChatMessage(
                                        "server",
                                        "No nickname assigned! Your nickname will be " + cC.getNickname()));
                            }
                            /* Add to channel */
                            channel.addClient(cC);
                            writeMessage(cC, new ChatMessage(
                                    "server",
                                    "Joined channel " + channelName));
                            ChatServer.report("client: " + cC.getNickname() + " joined channel: " + channelName, 1);
                        } // end of join channel

                    } // end of if stmt channel connection

                    /* valid leave commands can be '/leave' or '/leave <channelname>' */
                    else if (currCmd.startsWith("/leave") && channel != null && leaveOptionalArgSpace(currCmd)) {
                        String[] leaveArgs = currCmd.split(" ");
                        if (leaveArgs.length == 1) {
                            ChatServer.report("client: " + cC.getNickname() + " left channel: " + channelName, 1);
                            leaveChannel();
                        } else {
                            String channelNameArg =  currCmd.substring(7).trim();
                            if (!channelNameArg.equals(channel.getName())) {
                                ChatServer.report("client: " + cC.getNickname() + " failed to leave channel due to not being in that channel", 1);
                                writeMessage(cC, new ChatMessage("server",
                                "You can't leave a non-existent channel genius."));         
                            } else {
                                ChatServer.report("client: " + cC.getNickname() + " left channel: " + channelName, 1);
                                leaveChannel();
                            }
                        }
                    }

                    else if (currCmd.equals("/list")){
                        ChatServer.report("sending channel list to client: " + cC.getNickname(), 1);
                        writeMessage(cC, new ChatMessage("server", getChannelsInformation()));
                    }

                    else if (currCmd.equals("/stats")){
                        ChatServer.report("sending server stats to client: " + cC.getNickname(), 1);
                        writeMessage(cC, new ChatMessage("server", getServerStats()));
                    }

                    else {
                        if (channel != null) {// messages to channels
                            channel.sendMessage(new ChatMessage(cC.getNickname(), currCmd));

                        } else {// all other invalid commands, including improperly formatted ones
                            ChatServer.report("recieved unknown command from: " + cC.getNickname(), 1);
                            writeMessage(cC, new ChatMessage(
                                    "server",
                                    "Invalid command. If this was supposed to be a message, join a damn channel first."));
                        }
                    }

                    Thread.sleep(1000);
                } // end while
                ChatServer.report("ending ServerConnection to client thread", 1);

            } catch (IOException e) {
                /* Removes clients from lists and closes client connection */
                ChatServer.report("caught ioexeption", 0);
                try {
                    ChatServer.report("closed client at exception", 0);
                    leaveChannel();
                    removeClientFromList();
                    cC.closeConnection();
                } catch (IOException e1) {
                    ChatServer.report("a client was closed manually", 1);
                    e1.printStackTrace();
                }

            } catch (Exception e) {
                ChatServer.report("caught general exception", 0);
                e.printStackTrace();
            }
        }

        /**
         * @return a string with information about the channels running on the server
         */
        public String getChannelsInformation(){
            StringBuilder chanString = new StringBuilder();

            chanString.append("\n" + "_".repeat(69 + 25) + "\n");
            chanString.append(String.format("| %-25s|%-66s\n", "channel name", "number of users"));
            chanString.append("_".repeat(69 + 25) + "\n");
            for (Channel currChannel : channels) {
                chanString.append(String.format("| %-25s|%-66s\n", currChannel.getName(), currChannel.getNumUsers()));
                chanString.append("_".repeat(69 + 25) + "\n");
            }

            return chanString.toString();
        }

    }// end ServerConnection

}
