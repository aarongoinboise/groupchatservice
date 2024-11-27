import java.net.SocketException;

public class ChatServer2 {
    private static boolean shutdownHook = true;
    public static void main(String[] args) {
        int[] portAndDebug = ChatServerParser2.returnArgs(args);
        try {
            Server2 s = new Server2(portAndDebug[0], new Reporter2(portAndDebug[1]));
            /* Shutdown hook part, happens during ctrl-c */
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (shutdownHook) {
                    System.out.println("Shutting down server...");
                }
            }));
            s.run();

        } catch (SocketException e) {
            shutdownHook = false;
            ChatServerParser2.printUsageAndExit("Socket exception.");

        } catch (Exception e1) {
            ChatServerParser2.printUsageAndExit("Java setup is messed up, or port number is invalid.");

        }
    }
}
