import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server2 extends Thread {
    public static final ExecutorService pool = Executors.newFixedThreadPool(3);
    private ServerSocket serverSocket;
    private Reporter2 reporter;

    public Server2(int port, Reporter2 reporter) throws IOException {
        serverSocket = new ServerSocket(port);
        this.reporter = reporter;
    }

    @Override
    public void run() {
        reporter.report("Server " + serverSocket.getInetAddress() + " up on port " + serverSocket.getLocalPort() + " waiting for clients...", 1);
        System.out.println("test");
        while (true) {
            try (Socket client = serverSocket.accept()) {
                ClientConnection2 cC = new ClientConnection2(
                        new ObjectInputStream(client.getInputStream()),
                        new ObjectOutputStream(client.getOutputStream()));

                ServerConnection2 serverConnection = new ServerConnection2(cC);
                pool.execute(serverConnection);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class ServerConnection2 extends Thread {
        private ClientConnection2 cC;
        private CommandHelper2 cmdHelp;

        private ServerConnection2(ClientConnection2 cC) {
            this.cC = cC;
            cmdHelp = new CommandHelper2(reporter, cC);
        }

        @Override
        public void run() {
            try {
                while (cC.isOpen()) {
                    String currCmd = (String) cC.in.readObject();
                    reporter.report("received message from client: " + cC.getNickname(), 1);
                    if (currCmd.equals("/help")) {
                        cmdHelp.help();
                    }
                }
            } catch (IOException e) {

            }catch (Exception e) {
            }
        }
    }
}
