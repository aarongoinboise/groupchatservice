import java.net.SocketException;

public class ChatServer2 {
    private static boolean shutdownHook = true;
    public static void main(String[] args) {
        int[] portAndDebug = ChatServerParser2.returnArgs(args);
        Reporter2 reporter = new Reporter2(portAndDebug[1]);
        try {
            Server2 s = new Server2(portAndDebug[0], reporter);
            /* Shutdown hook part, happens during ctrl-c */
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (shutdownHook) {
                    reporter.report("Shutting down server...", 1, "white");
                }
            }));
            s.startServer();

        } catch (SocketException e) {
            shutdownHook = false;
            ChatServerParser2.printUsageAndExit("3 minutes idle, shutting down.");

        } catch (Exception e1) {
            ChatServerParser2.printUsageAndExit("Java setup is messed up, or port number is invalid.");

        }
    }
}
