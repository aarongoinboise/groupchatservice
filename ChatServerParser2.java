public class ChatServerParser2 {
    private static Reporter2 reporter = new Reporter2(1);

    public static int[] returnArgs(String[] args) {
        int[] portAndDebug = new int[2];
        String usage = "usage: java ChatServer -p <port#> -d <debug-level>\n where debug-level is 0 or 1";
        if (args.length != 4 || !args[0].equals("-p") || !args[2].equals("-d")) {
            printUsageAndExit(usage);
        }
        char[] portChars = args[1].toCharArray();
        for (char c : portChars) {
            if (!Character.isDigit(c)) {
                printUsageAndExit(usage);
            }
        }
        portAndDebug[0] = Integer.parseInt(args[1]);
        char[] debugChar = args[3].toCharArray();
        if (debugChar.length != 1) {
            printUsageAndExit(usage);
        }
        if (!Character.isDigit(debugChar[0])) {
            printUsageAndExit(usage);
        }
        int possDebugLevel = Integer.parseInt(args[3]);
        if (possDebugLevel != 0 && possDebugLevel != 1) {
            printUsageAndExit(usage);
        }
        portAndDebug[1] = possDebugLevel;
        return portAndDebug;
    }

    public static void printUsageAndExit(String msg) {
        reporter.report(msg, 0, "cyan");
        System.exit(1);
    }
}
