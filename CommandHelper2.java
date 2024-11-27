import java.io.IOException;

public class CommandHelper2 {
    Reporter2 reporter;
    private ClientConnection2 cC;
    private String[] cmds = { "/connect", "/nick", "/list",
            "/join", "/leave", "/quit", "/help" };
    private String helpMessage = "help";

    public CommandHelper2(Reporter2 reporter, ClientConnection2 cC) {
        this.reporter = reporter;
        this.cC = cC;
    }

    public void help() throws IOException {
        reporter.report("sending help message to client: " + cC.getNickname(), 1);
        cC.writeMessage(helpMessage);
    }
}
