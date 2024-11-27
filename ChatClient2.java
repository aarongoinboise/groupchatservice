import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatClient2 {
    public static ExecutorService pool;
    public static void main(String[] args) {
        if (args.length != 0) {
            ChatServerParser2.printUsageAndExit("usage: java ChatClient\nNo arguments.");
        }
        try (Scanner commandInput = new Scanner(System.in)) {
            while (true) {
                CommandHelper2.helpMsg();
                System.out.print("Enter a command: ");
                String fullCmd= commandInput.nextLine();
                Scanner cmdScan = new Scanner(fullCmd);
                cmdScan.useDelimiter("\\s+");
                String cmd = cmdScan.next();
                if (cmd.equals("/connect")) {
                    String serverName = cmdScan.next();
                    String pString = cmdScan.next();
                    ChatClientCmder2.connect(serverName, pString);
                }
                cmdScan.close();
            }
        } catch (Exception e) {
            ChatServerParser2.printUsageAndExit("Refer to the possible commands, and check server names.");
        }
    }
}
