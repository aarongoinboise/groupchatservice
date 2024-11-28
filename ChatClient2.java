import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class ChatClient2 {
    public static final String CONNECT_COMMAND = "/connect ";
    public static final String QUIT_COMMAND = "/quit";
    public static final String HELP_COMMAND = "/help";
    private static Scanner inputScanner = new Scanner(System.in);
    private static boolean connected = false;
    private static String userInput;

    public static void main(String[] args) {
        System.out.println("Type '/connect <host> <port>' to start:");

        while (true) {
            userInput = inputScanner.nextLine();

            if (userInput.startsWith(CONNECT_COMMAND)) {
                String[] commandParts = userInput.split(" ");
                if (commandParts.length < 3) {
                    System.out.println("Connection failed, unable to parse host/port");
                    continue;
                }
                String serverAddress = commandParts[1];
                int portNumber;
                try {
                    portNumber = Integer.parseInt(commandParts[2]);
                } catch (NumberFormatException e) {
                    System.out.println("Connection failed, invalid port");
                    continue;
                }
                connectToServer(serverAddress, portNumber);
            } else if (userInput.equals(QUIT_COMMAND)) {
                break;
            } else if (userInput.equals(HELP_COMMAND)) {
                printHelp();
            } else {
                System.out.println("Command not recognized, type '/help' for a list of commands");
            }
        }
    }

    private static void connectToServer(String serverAddress, int port) {
        try (Socket clientSocket = new Socket(serverAddress, port);
             ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream())) {

            connected = true;
            System.out.println("Connected to server at " + serverAddress + " on port " + port);

            while (connected) {
                // Check for user input to send
                if (inputScanner.hasNextLine()) {
                    String message = inputScanner.nextLine();
                    out.writeObject(message);
                    out.flush();
                    if (message.equals(QUIT_COMMAND)) {
                        connected = false;
                    }
                }

                // Check for incoming messages
                if (in.available() > 0) {
                    String message = (String) in.readObject();
                    System.out.println("Server: " + message);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            connected = false;
        }
    }

    private static void printHelp() {
        System.out.println("Available commands:");
        System.out.println(CONNECT_COMMAND + "<server-name> <port#> : Connect to server");
        System.out.println(QUIT_COMMAND + " : Close client");
        System.out.println(HELP_COMMAND + " : Show help");
    }
}
