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
                ObjectInputStream in = (ObjectInputStream) client.getInputStream();
                ObjectOutputStream out = (ObjectOutputStream) client.getOutputStream();

                ServerConnection2 serverConnection = new ServerConnection2(in, out);
                nickNameCounter++;
                pool.execute(serverConnection);  // Executes the task using the thread pool
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // ServerConnection2 implements Runnable instead of extending Thread
    private class ServerConnection2 implements Runnable {
        private ObjectInputStream in;
        private ObjectOutputStream out;

        private ServerConnection2(ObjectInputStream in, ObjectOutputStream out) {
            this.in = in;
            this.out = out;
        }

        @Override
        public void run() {
            reporter.report("new client connection: " + cC.getNickname(), 1);
            try {
                while (cC.isOpen()) {
                    System.out.println("before reading command");
                    String currCmd = (String) cC.in.readObject();
                    System.out.println("after reading command");
                    reporter.report("received message from client: " + cC.getNickname(), 1);
                    if (currCmd.equals("/help")) {
                        cmdHelp.help();
                        reporter.report("sent help message to client " + cC.getNickname(), 1);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
