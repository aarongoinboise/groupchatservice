import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;



/**
 * Client side class for a chat server which color codes messages
 */
public class ColorChatClient {
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    public static final int NUM_THREADS = 2;//one thread for sending, one thread for recieving
    public static ExecutorService pool;
    public static final String CONNECT_COMMAND = "/connect ";
    public static final String HELP_COMMAND = "/help";
    public static final String QUIT_COMMAND = "/quit";
    public static Scanner inputScanner;
    static boolean connected;//whether there is currently a connection to a server
    static boolean scannerInServer;
    static String userInput;

        /**
         * Main for ChatClient: Connects to a server and uses protocol commands
         * 
         * @param args
         */
        public static void main(String args[]){
        connected = false;
        scannerInServer = false;
        inputScanner = new Scanner(System.in);
        printHelp();
        while(true){
            /* Wait for scanned input to finish */
            if (!scannerInServer) {
                userInput = inputScanner.nextLine();
                /* Connects and starts threads for sending and receiving messages */
                if(userInput.startsWith(CONNECT_COMMAND)){
                    String serverAddress = userInput.substring(CONNECT_COMMAND.length()).split(" ")[0];
                    int portNumber;
                    try{
                        portNumber = Integer.parseInt(userInput.substring(CONNECT_COMMAND.length()).split(" ")[1]);
                    } 
                    catch(NumberFormatException | ArrayIndexOutOfBoundsException e){
                        System.out.println("connection failed, unable to parse port#");
                        continue;
                    }
                    /* Tries to start send/receive message threads */
                    pool = Executors.newFixedThreadPool(NUM_THREADS);
                    new ColorChatClient().serverConnect(serverAddress, portNumber);
                }// end connect command

                else if (userInput.equals(QUIT_COMMAND)){//exit
                    break;
                }
                else if (userInput.equals(HELP_COMMAND)){
                    printHelp();
                }
                else{
                    System.out.println("command not recognized, type '/help' for a list of commands");
                }
            } else {
                scannerInServer = false;
                printHelp();
            }
        }
    }

    /**
     * Connects to a chat server, sending, recieving and printing messages and commands appropiately
     * 
     * @param serverAddress address of server to connect to
     * @param port port to connect to
     */
    public void serverConnect(String serverAddress, int port) {
        Socket clientSocket;
        ObjectOutputStream out;
        ObjectInputStream in;
        try {
            clientSocket = new Socket(serverAddress, port);
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            in = new ObjectInputStream(clientSocket.getInputStream());
            connected = true;
        } catch (IOException e) {
            System.out.println("problem connecting to the server");
            return;
        }
        /* At this point, connection is established. Send/Receive threads are started */
        System.out.println("Connected to server " + serverAddress + " at port " + port);
        pool.execute(new MessageReciever(in));
        pool.execute(new MessageSender(out));

        /* Stays conneted */
        while(connected){
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                System.out.println("main interupted");
                e.printStackTrace();
            }
        }

        pool.shutdown();

        /* Waits for all tasks in send/receive to finish, then closes socket */
        try {
            while (!pool.awaitTermination(1, TimeUnit.MILLISECONDS));
            clientSocket.close();
        } catch (IOException | InterruptedException e) {
            System.out.println("problem closing socket");
            e.printStackTrace();
        }

    }

    protected MessageReciever getMessageReciever(ObjectInputStream in){
        return new MessageReciever(in);
    }

    /**
     * Prints help message relevant to the client only
     */
    private static void printHelp(){
        StringBuilder helpString = new StringBuilder();

        helpString.append("Available client commands: ");
        helpString.append("\n" + "_".repeat(32 + 55) + "\n");
        helpString.append(String.format("| %-32s|%-55s\n", "COMMAND", "DESCRIPTION"));
        helpString.append("_".repeat(32 + 55) + "\n");
        helpString.append(String.format("| %-32s|%-55s\n", CONNECT_COMMAND + "<server-name> <port#>", "connect to server at address <server-name>"));
        helpString.append("_".repeat(32 + 55) + "\n");
        helpString.append(String.format("| %-32s|%-55s\n", QUIT_COMMAND, "close chat client"));
        helpString.append("_".repeat(32 + 55) + "\n");
        helpString.append(String.format("| %-32s|%-55s\n", HELP_COMMAND, "print out a help message"));
        helpString.append("_".repeat(32 + 55) + "\n");

        System.out.print(helpString.toString());
    }

    /**
     * runnable class which waits for user input (a message to be sent on the chat server) and sends it to the chat server
     */
    private class MessageSender implements Runnable {
        ObjectOutputStream outputStream;

        /**
         * Constructor: sets, OOS and scanner
         * 
         * @param outputStream vehicle that sends messages to server
         * @param inputScanner same scanner as in main, scans user input to send msgs to server
         */
        public MessageSender(ObjectOutputStream outputStream){
            this.outputStream = outputStream;
        }

        @Override
        public void run() {
            while(connected){
                scannerInServer = true;
                userInput = inputScanner.nextLine();
                try {
                    outputStream.writeObject(new ChatMessage(userInput));
                    outputStream.flush();
                    if(userInput.equals(QUIT_COMMAND)){
                        connected = false;
                    }

                } catch (IOException e) {
                    connected = false;
                }
            }
            printHelp();
        }

    }

    /**
     * runnable class which waits for messages from the chat server and displays them to the user
     */
    protected class MessageReciever implements Runnable {
        ObjectInputStream inputStream;
        Queue<String> textColors;
        Map<String, String> nameColors;

        /**
         * Constructor: sets inputStream
         * 
         * @param inputStream
         */
        public MessageReciever(ObjectInputStream inputStream){
            this.inputStream = inputStream;

            nameColors = new HashMap<String, String>();

            textColors = new LinkedList<String>();
            textColors.add(ANSI_RED);
            textColors.add(ANSI_GREEN);
            textColors.add(ANSI_YELLOW);
            textColors.add(ANSI_BLUE);
            textColors.add(ANSI_PURPLE);
            textColors.add(ANSI_CYAN);
            textColors.add(ANSI_WHITE);
        }

        @Override
        public void run() {
            ChatMessage message;
            while(connected){
                try {
                    message = (ChatMessage) inputStream.readObject();
                } catch (ClassNotFoundException | IOException e) {
                    connected = false;
                    break;
                }

                if(!nameColors.containsKey(message.getSender())){
                    nameColors.put(message.getSender(), textColors.peek());
                    textColors.add(textColors.remove());
                }

                System.out.println(nameColors.get(message.getSender()) + message.getSender() + ": " + message.getMessage() + ANSI_RESET);
            }
        }


    }
}
