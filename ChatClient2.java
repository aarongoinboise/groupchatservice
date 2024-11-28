import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ChatClient2 {
    private static ExecutorService pool;
    private static final String CONNECT_COMMAND = "/connect ";
    private static final String HELP_COMMAND = "/help";
    private static final String QUIT_COMMAND = "/quit";
    private static Scanner inputScanner = new Scanner(System.in);
    private static boolean connected = false;

    public static void main(String[] args) {
        System.out.println("Type '/connect <host> <port>' to start:");

        while (true) {
            String userInput = inputScanner.nextLine();
            if (userInput.startsWith(CONNECT_COMMAND)) {
                String[] commandParts = userInput.substring(CONNECT_COMMAND.length()).split(" ");
                if (commandParts.length != 2) {
                    System.out.println("Invalid connection parameters");
                    continue;
                }
                String serverAddress = commandParts[0];
                int portNumber;
                try {
                    portNumber = Integer.parseInt(commandParts[1]);
                    pool = Executors.newFixedThreadPool(2);
                    new ChatClient().serverConnect(serverAddress, portNumber);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid port number");
                }
            } else if (userInput.equals(QUIT_COMMAND)) {
                break;
            } else if (userInput.equals(HELP_COMMAND)) {
                printHelp();
            } else {
                System.out.println("Command not recognized, type '/help' for a list of commands");
            }
        }
    }

    public void serverConnect(String serverAddress, int port) {
        try (Socket clientSocket = new Socket(serverAddress, port);
             ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream())) {

            connected = true;
            System.out.println("Connected to server " + serverAddress + " at port " + port);
            pool.execute(new MessageReceiver(in));
            pool.execute(new MessageSender(out));

            while (connected) {
                Thread.sleep(1);
            }

            pool.shutdown();
            pool.awaitTermination(1, TimeUnit.MILLISECONDS);
        } catch (IOException | InterruptedException e) {
            System.out.println("Connection failed or error closing socket");
            e.printStackTrace();
        }
    }

    private static void printHelp() {
        System.out.println("Available client commands:\n" +
                "/connect <server-name> <port#> - Connect to server\n" +
                "/quit - Close client\n" +
                "/help - Print help message");
    }

    private class MessageSender implements Runnable {
        private ObjectOutputStream outputStream;

        public MessageSender(ObjectOutputStream outputStream) {
            this.outputStream = outputStream;
        }

        @Override
        public void run() {
            while (connected) {
                String userInput = inputScanner.nextLine();
                try {
                    outputStream.writeObject(new ChatMessage(userInput));
                    outputStream.flush();
                    if (userInput.equals(QUIT_COMMAND)) {
                        connected = false;
                    }
                } catch (IOException e) {
                    connected = false;
                }
            }
        }
    }

    private class MessageReceiver implements Runnable {
        private ObjectInputStream inputStream;

        public MessageReceiver(ObjectInputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            while (connected) {
                try {
                    ChatMessage message = (ChatMessage) inputStream.readObject();
                    System.out.println(message.getSender() + ": " + message.getMessage());
                } catch (ClassNotFoundException | IOException e) {
                    connected = false;
                }
            }
        }
    }
}
