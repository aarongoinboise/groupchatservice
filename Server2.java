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
    private ClientInfo2 clientInfo;
    private String[] cmds = { "/connect", "/nick", "/list",
    "/join", "/leave", "/quit", "/help" };

    public Server2(int port, Reporter2 reporter) throws IOException {
        serverSocket = new ServerSocket(port);
        clientInfo = new ClientInfo2();
    }

    @Override
    public void run() {
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

        private ServerConnection2(ClientConnection2 cC) {
            this.cC = cC;
        }
    }
}
