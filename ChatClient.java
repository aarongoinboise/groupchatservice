import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Client side class for a chat client
 */
public class ChatClient {
    public static Scanner inputScanner;
    public static Scanner sockScan;
    private static Reporter reporter = new Reporter(1);
    private static boolean inChannel;

    /**
     * Main method for ChatClient: Connects to a server and uses protocol commands
     * 
     * @param args command line arguments
     */
    @SuppressWarnings("resource")
    public static void main(String args[]) {
        inputScanner = new Scanner(System.in);
        while (true) {
            String connectCmd;
            reporter.report("Type '/connect <host> <port>' to start:", 1, "green");
            try {
                connectCmd = inputScanner.nextLine();
            } catch (IndexOutOfBoundsException e) {
                continue;
            }
            Socket socket = null;
            try {
                sockScan = new Scanner(connectCmd); // used to parse through connect command
                sockScan.useDelimiter("\\s+");
                String cmd = sockScan.next();
                if (!cmd.equals("/connect")) { // should use connect as the first command
                    reporter.report("Not a connect command, try again.", 1, "cyan");
                    continue;
                }
                String host = sockScan.next();
                String portS = sockScan.next();
                char[] cs = portS.toCharArray();
                if (sockScan.hasNext()) {
                    sockScan.close();
                    reporter.report("Bad connect command, try again.", 1, "cyan");
                    continue;
                }
                sockScan.close();
                for (char c : cs) {
                    if (!Character.isDigit(c)) {
                        reporter.report("Bad connect command, try again.", 1, "cyan");
                        continue;
                    }
                }
                int port = Integer.parseInt(portS);
                try {
                    socket = new Socket(host, port);
                } catch (IOException e) {
                    reporter.report("Bad connect command, try again.", 1, "cyan");
                    continue;
                }
            } catch (NoSuchElementException e) {
                reporter.report("Bad connect command, try again.", 1, "cyan");
                continue;
            }
            boolean connected = true;
            try {
                inChannel = false;
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                reporter.report("Connection with server " + socket.getInetAddress() + " established!", 1, "green");
                reporter.report("Current nickname: " + ((StringObject) in.readObject()).toString()
                        + ". To change, use the /nick command", 1, "yellow");

                while (connected) {
                    if (socket.isClosed()) {
                        connected = false;
                        continue;
                    }

                    String possMsgs = ((StringObject) in.readObject()).toString();
                    if (possMsgs.equals("No new messages...")) {
                        reporter.report(possMsgs, 1, "blue");
                    } else if (!possMsgs.isBlank()) { // used primarily when the client is not in a channel
                        reporter.report(possMsgs, 1, "set");
                    }
                    if (!inChannel) {
                        reporter.report("Enter a command: ", 1, "purple");
                    } else {
                        reporter.report("Enter a message to add to the channel or a command: ", 1, "purple"); // used when in a channel
                    }
                    String currCmd = inputScanner.nextLine();

                    StringObject serializedCmd = new StringObject(currCmd);
                    out.writeObject(serializedCmd);
                    out.flush();

                    if (currCmd.startsWith("/connect")) {
                        reporter.report("Already connected to server, you must disconnect first.", 1, "red");
                        continue;
                    }
                    String response = ((StringObject) in.readObject()).toString();
                    if (response.contains("Leaving server")) {
                        connected = false;
                    }
                    if (response.contains("Chat messages will now display.")) {
                        inChannel = true;
                    }
                    if (response.contains("left channel")) {
                        inChannel = false;
                    }
                    reporter.report(response, 1, "random");
                } // end connected while

            } catch (IOException | ClassNotFoundException e) {
                reporter.report("Possible server shutdown or server is full, connection ended.", 1, "red");
                try {
                    socket.close();
                } catch (IOException e1) {
                }
            }
        } // end client while

    }// end main

}