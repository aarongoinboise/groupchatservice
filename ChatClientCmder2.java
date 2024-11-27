import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatClientCmder2 {
    public static ExecutorService pool;
    private boolean connected;

    public ChatClientCmder2() {

    }
    
    public static void connect(String serverName, String pString) throws Exception {
        char[] ps = pString.toCharArray();
        for (char c : ps) {
            if (!Character.isDigit(c)) {
                throw new Exception();
            }
        }
        int port = Integer.parseInt(pString);
        pool = Executors.newFixedThreadPool(2);
        Socket clientSocket;
        ObjectOutputStream out;
        ObjectInputStream in;
        try {
            clientSocket = new Socket(serverName, port);
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            in = new ObjectInputStream(clientSocket.getInputStream());
        } catch (IOException e) {
            System.out.println("problem connecting to the server");
            e.printStackTrace();
            return;
        }
        System.out.println("Connected to server " + serverName + " at port " + port);
        pool.execute(new MessageReceiver(in));
        pool.execute(new MessageSender(out));
    }

    /**
     * runnable class which waits for user input (a message to be sent on the chat server) and sends it to the chat server
     */
    private static class MessageSender implements Runnable {
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
        }

    }

    /**
     * runnable class which waits for messages from the chat server and displays them to the user
     */
    private static class MessageReceiver implements Runnable {
        ObjectInputStream inputStream;

        /**
         * Constructor: sets inputStream
         * 
         * @param inputStream
         */
        public MessageReceiver(ObjectInputStream inputStream){
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            ChatMessage message;
            while(connected){
                try {
                    message = (ChatMessage) inputStream.readObject();
                    System.out.println(message.getSender() + ": " + message.getMessage());
                } catch (ClassNotFoundException | IOException e) {
                    connected = false;
                    break;
                }
            }
        }


    }
}
