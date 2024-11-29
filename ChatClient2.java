import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.NoSuchElementException;
import java.util.Scanner;

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
    @SuppressWarnings("resource")
    public static void main(String args[]) {
        inChannel = false;
        while (true) {
            inputScanner = new Scanner(System.in);
            System.out.println("Type '/connect <host> <port>' to start:");
            String connectCmd = inputScanner.nextLine();
            Socket socket = null;
            try {
                Scanner sockScan = new Scanner(connectCmd);
                sockScan.useDelimiter("\\s+");
                String cmd = sockScan.next();
                if (!cmd.equals("/connect")) {
                    System.out.println("Not a connect command, try again.");
                    continue;
                }
                String host = sockScan.next();
                String portS = sockScan.next();
                char[] cs = portS.toCharArray();
                if (sockScan.hasNext()) {
                    sockScan.close();
                    System.out.println("Bad connect command, try again.");
                    continue;
                }
                sockScan.close();
                for (char c : cs) {
                    if (!Character.isDigit(c)) {
                        System.out.println("Bad connect command, try again.");
                        continue;
                    }
                }
                int port = Integer.parseInt(portS);
                try {
                    socket = new Socket(host, port);
                } catch (IOException e) {
                    System.out.println("Bad connect command, try again.");
                    continue;
                }
            } catch (NoSuchElementException e) {
                System.out.println("Bad connect command, try again.");
                continue;
            }
            boolean connected = true;
            try {
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                System.out.println("Connection with server " + socket.getInetAddress() + " established!");
                nickname = ((StringObject2) in.readObject()).toString();
                System.out.println("Current nickname: " + nickname + ". To change, use the /nick command");
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
                    if (response.startsWith("joined existing channel") || response.startsWith("created a new channel")) {
                        inChannel = true;

                    } else if (response.contains("left channel") && !response.contains("Leaving server")) {
                        inChannel = false;
                    
                    } else if (response.contains("Leaving server")) {
                        inChannel = false;

                    } else if (response.startsWith("your new nickname is")) {
                        nickname = cmd.substring(6);
                    }
                    System.out.println(response);
                }

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

}