import java.net.SocketException;

public class ChatServer {
    public static void main(String[] args) {
        int[] portAndDebug = ChatServerParser.returnArgs(args);
        Reporter reporter = new Reporter(portAndDebug[1]);
        try {
            GetServed server = new GetServed(portAndDebug[0], reporter);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                reporter.report("Shutting down server...", 1, "white");
            }));
            server.youGotServed();

        } catch (SocketException e) {
            ChatServerParser.printUsageAndExit("3 minutes idle, shutting down.");

        } catch (Exception e) {
            ChatServerParser.printUsageAndExit("Java setup is messed up, or port number is invalid.");
        }
    }
}
