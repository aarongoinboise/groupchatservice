import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Client side class for a chat server
 */
public class ChatClient {
    public static Scanner inputScanner;
    public static Scanner sockScan;
    private static Reporter reporter = new Reporter(1);
    // public static String nickname;

    /**
     * Main for ChatClient: Connects to a server and uses protocol commands
     * 
     * @param args
     */
    @SuppressWarnings("resource")
    public static void main(String args[]) {
        inputScanner = new Scanner(System.in);
        while (true) {
            String connectCmd;
            reporter.report("Type '/connect <host> <port>' to start:", 1, "green");
            connectCmd = inputScanner.nextLine();
            Socket socket = null;
            try {
                sockScan = new Scanner(connectCmd);
                sockScan.useDelimiter("\\s+");
                String cmd = sockScan.next();
                if (!cmd.equals("/connect")) {
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
                    socket.setSoTimeout(5000);
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
                    if (!possMsgs.isBlank()) {
                        reporter.report(possMsgs, 1, "set");
                    }
                    String currCmd = getUserInputWithTimeout(5);
                    if (currCmd == null) {
                        continue;
                    }

                    if (currCmd.startsWith("/connect")) {
                        reporter.report("Already connected to server, you must disconnect first.", 1, "red");
                        continue;
                    }
                    StringObject serializedCmd = new StringObject(currCmd);
                    out.writeObject(serializedCmd);
                    out.flush();
                    String response = ((StringObject) in.readObject()).toString();
                    // check responses, which will change the protocols
                    // if (response.startsWith("joined existing channel")
                    // || response.startsWith("created a new channel")) {

                    // } else if (response.contains("left channel") && !response.contains("Leaving
                    // server")) {

                    // } else
                    if (response.contains("Leaving server")) {
                        connected = false;

                    }
                    reporter.report(response, 1, "random");
                } // end connected while

            } catch (IOException | ClassNotFoundException e) {
                reporter.report("Possible server shutdown, connection ended.", 1, "red");
            }
        } // end client while

    }// end main

    private static String getUserInputWithTimeout(int timeoutSeconds) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(() -> inputScanner.nextLine());
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            return null; // Input timed out
        } catch (Exception e) {
            return null; // Other error
        } finally {
            executor.shutdownNow();
        }
    }

}