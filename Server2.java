import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server2 {
    public static final ExecutorService pool = Executors.newFixedThreadPool(4);
    private ServerSocket serverSocket;
    private Reporter2 reporter;
    private int nickNameIdx;
    private String[] currNicknames;
    private ArrayList<ChannelInfo> channels;

    public Server2(int port, Reporter2 reporter) throws IOException {
        serverSocket = new ServerSocket(port);
        this.reporter = reporter;
        nickNameIdx = 0;
        currNicknames = new String[4];
        channels = new ArrayList<ChannelInfo>();
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
                        // check if message is to be sent
                    
                    } else { 
                        reporter.report("client " + currNickname + " send command " + currCmd, 1);
                        if (currCmd.equals("/help")) {
                            out.writeObject("help message placeholder");
                            out.flush();
                            reporter.report("sent help message to client " + currNickname, 1);
                        } else if (currCmd.startsWith("/nick") && currCmd.length() >= 7) {
                            String newNickname = currCmd.substring(6);
                            // check if newnickname is unique among all users
                            for (String n : currNicknames) {
                                if (n.equals(newNickname)) {
                                    out.writeObject("the new nickname is not unique, try again");
                                    out.flush();
                                    reporter.report(currNickname + " attempted to change nickname into a non-unique value",
                                            1);
                                }
                            }
                            // at this point, change the nickname
                            String oldNickname = currNicknames[nickNameIdx];
                            currNicknames[nickNameIdx] = newNickname;
                            out.writeObject("your new nickname is " + newNickname);
                            out.flush();
                            reporter.report(oldNickname + " changed the nickname to " + newNickname,
                                    1);

                        } else {
                            out.writeObject("bad command, try again");
                            out.flush();
                            reporter.report("sent retry message to client " + currNickname, 1);
                        }
                    }
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
        HashMap<String, String> messages;

        private ChannelInfo(String name, String firstMember) {
            this.name = name;
            members = new ArrayList<String>();
            members.add(firstMember);
            messages = new HashMap<String, String>();
        }

        private void addMessage(String message) {
            for (String member : members) {
                messages.put(member, message);
            }
        }
    }
}
