import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Client side class for a chat server
 */
public class ChatClient2 {
    public static final int NUM_THREADS = 2;// one thread for sending, one thread for recieving
    public static ExecutorService pool;
    public static final String CONNECT_COMMAND = "/connect ";
    public static final String HELP_COMMAND = "/help";
    public static final String QUIT_COMMAND = "/quit";
    public static Scanner inputScanner;
    static boolean connected;// whether there is currently a connection to a server
    static boolean scannerInServer;
    static String userInput;

    /**
     * Main for ChatClient: Connects to a server and uses protocol commands
     * 
     * @param args
     */
    public static void main(String args[]) {
        connected = false;
        scannerInServer = false;
        inputScanner = new Scanner(System.in);
        System.out.println("Type '/connect <host> <port>' to start:");
        String connectCmd = "";
        Socket socket = null;
        while (!connectCmd.startsWith("/connect")) {
            connectCmd = inputScanner.nextLine();
            if (!connectCmd.startsWith("/connect")) {
                System.out.println("Not a connect command");
            } else {
                socket = trySocket(connectCmd);
                if (socket == null) {
                    System.out.println("Connection could not be established");
                    connectCmd = "";
                }
            }
        } // end while
        try {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            System.out.println("Connection with server " + socket.getInetAddress() + " established!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Socket trySocket(String connectCmd) {
        try {
            Scanner sockScan = new Scanner(connectCmd);
            sockScan.useDelimiter("\\s+");
            sockScan.next();
            String host = sockScan.next();
            String portS = sockScan.next();
            char[] cs = portS.toCharArray();
            if (sockScan.hasNext()) {
                sockScan.close();
                return null;
            }
            sockScan.close();
            for (char c : cs) {
                if (!Character.isDigit(c)) {
                    return null;
                }
            }
            int port = Integer.parseInt(portS);
            try (Socket socket = new Socket(host, port)) {
                return new Socket(host, port);
            } catch (IOException e) {
                return null;
            }
        } catch (NoSuchElementException e) {
            return null;
        }
    }

}