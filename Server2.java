import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server2 {
    public static final ExecutorService pool = Executors.newFixedThreadPool(3);
    private ServerSocket serverSocket;
    private Reporter2 reporter;
    private int nickNameCounter;

    public Server2(int port, Reporter2 reporter) throws IOException {
        serverSocket = new ServerSocket(port);
        this.reporter = reporter;
        nickNameCounter = 0;
    }

    public void startServer() {
        reporter.report("Server " + serverSocket.getInetAddress() + " up on port " + serverSocket.getLocalPort() + " waiting for clients...", 1);
        while (true) {
            try (Socket client = serverSocket.accept()) {
                ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(client.getInputStream());
                out.writeObject("default" + nickNameCounter);
                out.flush();
                reporter.report("new client connection: default" + nickNameCounter, 1);
                nickNameCounter++;

                ServerConnection2 serverConnection = new ServerConnection2(in, out);
                pool.execute(serverConnection);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // ServerConnection2 implements Runnable instead of extending Thread
    private class ServerConnection2 implements Runnable {
        private ObjectInputStream in;
        private ObjectOutputStream out;
        private String currNickname;

        private ServerConnection2(ObjectInputStream in, ObjectOutputStream out) {
            this.in = in;
            this.out = out;
            this.currNickname = "unknown";
        }

        @Override
        public void run() {
            boolean open = true;
            try {
                while (open) {
                    String currCmd = (String) in.readObject();
                    String currNickname = (String) in.readObject();
                    reporter.report("client " + currNickname + " send command " + currCmd, 1);
                    if (currCmd.equals("/help")) {
                        out.writeObject("help message placeholder");
                        out.flush();
                        reporter.report("sent help message to client " + currNickname, 1);
                    }
                }
            } catch (IOException e) {
                reporter.report("client " + currNickname + " disconnected", 0);
            } catch (Exception e) {
                reporter.report("exception " + e.toString() + " occurred", 0);
            }
        }
    }
}
