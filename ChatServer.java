import java.net.SocketException;

/**
 * Server side class for a chat server
 */
public class ChatServer {
    /**
     * Main method for ChatClient: Starts a server and accepts clients as they connect
     * 
     * @param args command line args
     */
    public static void main(String[] args) {
        int[] portAndDebug = ChatServerParser.returnArgs(args);
        Reporter reporter = new Reporter(portAndDebug[1]);
        try {
            GetServed server = new GetServed(portAndDebug[0], reporter);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                reporter.report("Shutting down server...", 1, "white");
            }));// shutdown hook
            server.youGotServed();

        } catch (SocketException e) {
            ChatServerParser.printUsageAndExit("3 minutes idle, shutting down.");

        } catch (Exception e) {
            ChatServerParser.printUsageAndExit("Java setup is messed up, or port number is invalid.");
        }
    }
}
