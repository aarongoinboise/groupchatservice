import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server2 {
    private static final ExecutorService pool = Executors.newFixedThreadPool(4);
    private ServerSocket serverSocket;
    private Reporter2 reporter;
    private int nickNameIdx;
    private String[] currNicknames;
    private ArrayList<ChannelInfo> channels;
    private String helpMsg;

    public Server2(int port, Reporter2 reporter) throws IOException {
        serverSocket = new ServerSocket(port);
        this.reporter = reporter;
        nickNameIdx = 0;
        currNicknames = new String[4];
        channels = new ArrayList<ChannelInfo>();
        helpMsg = "Command\tDescription\n" +
                "/connect <server-name> [port#]\tConnect to named server (port# optional)\n" +
                "/nick <nickname>\tPick a nickname (should be unique among active users)\n" +
                "/list\tList channels and number of users\n" +
                "/join <channel>\tJoin a channel, all text typed is sent to all users on the channel\n" +
                "/leave [<channel>]\tLeave the current (or named) channel\n" +
                "/quit\tLeave chat and disconnect from server\n" +
                "/help\tPrint out help message";
    }

    public void startServer() {
        reporter.report("Server " + serverSocket.getInetAddress() + " up on port " + serverSocket.getLocalPort()
                + " waiting for clients...", 1);
        while (true) {
            try (Socket client = serverSocket.accept()) {
                ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(client.getInputStream());
                out.writeObject("default" + nickNameIdx);
                out.flush();
                reporter.report("new client connection: default" + nickNameIdx, 1);
                currNicknames[nickNameIdx] = "default" + nickNameIdx;
                int currNNIdx = nickNameIdx;
                if (nickNameIdx == 3) {
                    nickNameIdx = 0;
                } else {
                    nickNameIdx++;
                }

                ServerConnection2 serverConnection = new ServerConnection2(in, out, currNNIdx);
                pool.execute(serverConnection);
            } catch (IOException e) {
                reporter.report("client default" + nickNameIdx + " disconnected", 0);
            }
        }
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

        @Override
        public void run() {
            boolean open = true;
            try {
                while (open) {
                    String currCmd = (String) in.readObject();

                    if (inChannel) {
                        // check if message is to be sent, continue, or to break out
                    }

                    reporter.report("client " + currNickname + " sent command " + currCmd, 1);
                    if (currCmd.equals("/help")) {
                        out.writeObject(helpMsg);
                        out.flush();
                        reporter.report("sent help message to client " + currNickname, 1);

                    } else if (currCmd.startsWith("/nick") && currCmd.length() >= 7
                            && !currCmd.substring(6).equals("")) {
                        String newNickname = currCmd.substring(6);
                        boolean unique = true;
                        // check if newnickname is unique among all users
                        for (String n : currNicknames) {
                            if (n.equals(newNickname)) {
                                out.writeObject("the new nickname is not unique, try again");
                                out.flush();
                                reporter.report(
                                        currNickname + " attempted to change nickname into a non-unique value",
                                        1);
                                unique = false;
                                break;
                            }
                        }
                        if (unique) {
                            // at this point, change the nickname
                            String oldNickname = currNicknames[nickNameIdx];
                            currNicknames[nickNameIdx] = newNickname;
                            out.writeObject("your new nickname is " + newNickname);
                            out.flush();
                            reporter.report(oldNickname + " changed the nickname to " + newNickname,
                                    1);
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
                                    for (String member : channel.members) {
                                        cInfo += member + "\n";
                                    }
                                    cInfo += "-\n";
                                }
                            }
                        }
                        out.writeObject(cInfo);
                        out.flush();
                    } else if (currCmd.startsWith("/join") && currCmd.length() >= 7
                            && !currCmd.substring(6).equals("")) {
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
                                channelToJoin.members.add(currNickname);
                                s1 = "joined existing channel " + channelToJoin.name;
                                reporter.report(
                                        currNickname + " joined existing channel " + channelToJoin.name,
                                        1);

                            } else { // create new channel
                                channelToJoin = new ChannelInfo(possChannelName, currNickname);
                                channels.add(channelToJoin);
                                s1 = "created a new channel called " + channelToJoin.name;
                                reporter.report(
                                        currNickname + " created a new channel called " + channelToJoin.name,
                                        1);
                            }
                            s1 += ". Chat messages will now display. Any non-command message you enter will now display in the channel. Commands will still work in addition to chat messages.";
                            out.writeObject(s1);
                            out.flush();
                            inChannel = true;
                        }

                    } else {
                        out.writeObject("bad command, try again");
                        out.flush();
                        reporter.report("sent retry message to client " + currNickname, 1);
                    } // end else
                }
            } catch (IOException e) {
                reporter.report("client " + currNickname + " disconnected", 0);
            } catch (Exception e) {
                reporter.report("exception " + e.toString() + " occurred", 0);
            }
        }
    }

    private class ChannelInfo {
        String name;
        ArrayList<String> members;
        HashMap<String, ArrayList<String>> messages;

        private ChannelInfo(String name, String firstMember) {
            this.name = name;
            members = new ArrayList<String>();
            members.add(firstMember);
            messages = new HashMap<>();
        }

        private void addMessage(String message) {
            for (String member : members) {
                messages.computeIfAbsent(member, messages -> new ArrayList<String>()).add(message);
            }
        }

        private String sendMessages(String member) {
            if (!messages.containsKey(member)) {
                return "";
            }
            ArrayList<String> memberMessages = messages.remove(member);
            return String.join("\n", memberMessages);
        }
    }
}
