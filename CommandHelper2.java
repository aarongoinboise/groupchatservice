import java.io.IOException;

public class CommandHelper2 {
    Reporter2 reporter;
    private ClientConnection2 cC;
    private static String[] cmds = { "/connect <server-name> [port#]", "/nick <nickname>", "/list",
                "/join <channel>", "/leave [<channel>]", "/quit", "/help" };
        private String helpMessage = "help";
    
        public CommandHelper2(Reporter2 reporter, ClientConnection2 cC) {
            this.reporter = reporter;
            this.cC = cC;
        }
    
        public void help() throws IOException {
            reporter.report("sending help message to client: " + cC.getNickname(), 1);
            cC.writeMessage(helpMessage);
        }
    
        public static void helpMsg() {
            String ret = "Possible commands: ";
            for (String s : cmds) {
            ret += s + ", ";
        }
        System.out.print(ret.substring(0, ret.length() - 2) + "\n");
    }
}
