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
    public static final int NUM_THREADS = 2;// one thread for sending, one thread for receiving
    public static Scanner inputScanner;
    public static boolean inChannel;
    public static String nickname;

    /**
     * Main for ChatClient: Connects to a server and uses protocol commands
     * 
     * @param args
     */
    public static void main(String args[]) {
        while (true) {
            inputScanner = new Scanner(System.in);
            System.out.println("Type '/connect <host> <port>' to start:");
            String connectCmd = inputScanner.nextLine();
            Socket socket = null;
            try {
                Scanner sockScan = new Scanner(connectCmd);
                sockScan.useDelimiter("\\s+");
                sockScan.next();
                String host = sockScan.next();
                String portS = sockScan.next();
                char[] cs = portS.toCharArray();
                if (sockScan.hasNext()) {
                    sockScan.close();
                    System.out.println("Bad connect command.");
                    continue;
                }
                sockScan.close();
                for (char c : cs) {
                    if (!Character.isDigit(c)) {
                        System.out.println("Bad connect command.");
                        continue;
                    }
                }
                int port = Integer.parseInt(portS);
                try {
                    socket = new Socket(host, port);
                } catch (IOException e) {
                    System.out.println("Bad connect command.");
                    continue;
                }
            } catch (NoSuchElementException e) {
                System.out.println("Bad connect command.");
                continue;
            }
            boolean connected = true;
            try {
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                System.out.println("Connection with server " + socket.getInetAddress() + " established!");
                StringObject2 result = (StringObject2) in.readObject();
                System.out.println(result.toString());
                String cmd = "";
                while (connected) {
                    cmd = inputScanner.nextLine();
                    if (cmd.startsWith("/connect")) {
                        System.out.println("Already connected to server, you must disconnect first.");
                        continue;
                    }
                    out.writeObject(cmd);
                    out.flush();
                    String response = (String) in.readObject();
                    // check responses, which will change the protocols
                    if (!response.startsWith("bad")) {
                        nickname = cmd.substring(6);
                    }
                    System.out.println(response);
                }

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    // /**
    // * runnable class which waits for messages from the chat server and displays
    // * them to the user
    // */
    // private class ChannelChatDisplay implements Runnable {
    // ObjectInputStream in;

    // /**
    // * Constructor: sets inputStream
    // *
    // * @param inputStream
    // */
    // public ChannelChatDisplay(ObjectInputStream in) {
    // this.in = in;
    // }

    // @Override
    // public void run() {
    // String message;
    // while (inChannel) {
    // try {
    // message = (String) in.readObject();
    // System.out.println(message);
    // } catch (ClassNotFoundException | IOException e) {
    // inChannel = false;
    // break;
    // }
    // }
    // }

    // }

}