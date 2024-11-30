import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Client side class for a chat server
 */
public class ChatClient2 {
    public static Scanner inputScanner;
    public static String nickname;

    /**
     * Main for ChatClient: Connects to a server and uses protocol commands
     * 
     * @param args
     */
    @SuppressWarnings("resource")
    public static void main(String args[]) {
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
                    socket.setSoTimeout(5000);
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
                String[] cmd = new String[1];
                while (connected) {
                    if (socket.isClosed()) {
                        connected = false;
                        continue;
                    }

                    String possMsgs = ((StringObject2) in.readObject()).toString();
                    if (!possMsgs.isBlank()) {
                        System.out.println(possMsgs);
                    }

                    // give user only 5 seconds to input
                    Timer timer = new Timer();
                    boolean[] timesUp = { false };
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            timesUp[0] = true;
                        }
                    }, 5000);
                    try {
                        Thread inputThread = new Thread(() -> {
                            cmd[0] = inputScanner.nextLine();
                        });
                        inputThread.start();
                        while (!timesUp[0] && cmd[0] == null)
                            ;
                        if (timesUp[0]) { // didn't scan in time
                            continue;
                        }
                        timer.cancel();
                    } catch (IndexOutOfBoundsException e) {
                        continue;
                    }

                    if (cmd[0].startsWith("/connect")) {
                        System.out.println("Already connected to server, you must disconnect first.");
                        continue;
                    }
                    StringObject2 serializedCmd = new StringObject2(cmd[0]);
                    out.writeObject(serializedCmd);
                    out.flush();
                    String response = ((StringObject2) in.readObject()).toString();
                    // check responses, which will change the protocols
                    if (response.startsWith("joined existing channel")
                            || response.startsWith("created a new channel")) {

                    } else if (response.contains("left channel") && !response.contains("Leaving server")) {

                    } else if (response.contains("Leaving server")) {
                        connected = false;

                    } else if (response.startsWith("your new nickname is")) {
                        nickname = cmd[0].substring(6);
                    }
                    System.out.println(response);

                } // end connected while

            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Possible server shutdown, connection ended.");
            }
        } // end client while
    }// end main

}