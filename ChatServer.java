import java.io.IOException;
import java.net.SocketException;

/**
 * Driver for server
 */
public class ChatServer {
    private static int debugLevel;
    private static boolean shutdownHook = true;

    /**
     * Prints usage message and exits.
     */
    private static void usage() {
        System.out.println("Usage: java ChatServer -p <port#> -d <debug-level>");
        System.out.println("Debug level 0: only error messages are reported");
        System.out.println("Debug level 1: all events are reported");
        System.exit(1);
    }

    /**
     * prints report if the provided level is <= debugLevel
     * 
     * @param report the report to be printed
     * @param level the lowest debug level for report to be printed
     */
    public static void report(String report, int level){
        if(level <= debugLevel){
            System.out.println(report);
        }
    }

    /**
     * Main driver. Parses args and starts a chat server.
     * Usage: java ChatServer -p <port#> -d <debug-level>
     * 
     * @param args the command line args for ChatServer
     */
    public static void main(String args[]) {
        debugLevel = 0;
        if (args.length != 4 || !args[0].equals("-p") || !args[2].equals("-d")) {
            usage();
        }
        String portNumber = args[1];
        

        try{
            debugLevel = Integer.parseInt(args[3]);
        } catch(NumberFormatException e){
            usage();
            return;
        }

        if(debugLevel < 0 || debugLevel > 1){
            usage();
            return;
        }

        try {
            Server s = new Server(Integer.parseInt(portNumber));
            /* Shutdown hook part, happens during ctrl-c */
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (shutdownHook) {
                    try {
                        s.sendStatsAndShutdown();
                        System.out.println("\nServer was shutdown.");

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }));
            s.runServer();

        } catch (SocketException e) {
            shutdownHook = false;
            System.out.println("Server shutdown due to 3 minutes of idle activity.");
            System.exit(1);

        } catch (NumberFormatException | IOException | ClassNotFoundException | InterruptedException e) {
            usage();

        }
    }
}
