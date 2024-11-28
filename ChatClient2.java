import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;



/**
 * Client side class for a chat server
 */
public class ChatClient2 {
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
        System.out.println("Type '/connect <host> <port>' to start:");
        while(true){
            
        } // end while
    }

}