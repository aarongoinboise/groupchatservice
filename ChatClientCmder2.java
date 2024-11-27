import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatClientCmder2 {
    public static ExecutorService pool;
    private static boolean connected;
    
        public ChatClientCmder2() {
            connected = false;
        }
        
        public static void connect(String serverName, String pString) throws Exception {
            char[] ps = pString.toCharArray();
            for (char c : ps) {
                if (!Character.isDigit(c)) {
                    throw new Exception();
                }
            }
            int port = Integer.parseInt(pString);
            pool = Executors.newSingleThreadExecutor(); 
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
            pool.execute(new ChatHandler(in, out));
        }
    
        private static class ChatHandler implements Runnable {
            private ObjectInputStream inputStream;
            private ObjectOutputStream outputStream;
            private Scanner inputScanner = new Scanner(System.in);
    
            public ChatHandler(ObjectInputStream inputStream, ObjectOutputStream outputStream) {
                this.inputStream = inputStream;
                this.outputStream = outputStream;
            }
    
            @Override
            public void run() {
                try {
                    while (connected) {
                    // Handle receiving messages from server
                    if (inputStream.available() > 0) {
                        ChatMessage message = (ChatMessage) inputStream.readObject();
                        System.out.println(message.getSender() + ": " + message.getMessage());
                    }

                    // Handle sending messages from user input
                    if (inputScanner.hasNextLine()) {
                        String userInput = inputScanner.nextLine();
                        outputStream.writeObject(new ChatMessage(userInput));
                        outputStream.flush();

                        if (userInput.equalsIgnoreCase("quit")) {
                            connected = false;
                        }
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                connected = false;
                System.out.println("Connection lost");
                e.printStackTrace();
            }
        }
    }
}
