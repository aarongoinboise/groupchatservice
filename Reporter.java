/**
 * Object for tracking the debugLevel and using the correct colorPrint commands
 */
public class Reporter {
    private int debugLevel;
    private TermColors tC;

    /**
     * Constructor: initializes values
     * 
     * @param debugLevel the debug level
     */
    public Reporter(int debugLevel) {
        this.debugLevel = debugLevel;
        tC = new TermColors();
    }

    /**
     * Uses correct colorPrint commands to print information on terminal
     * 
     * @param msg the message to print
     * @param msgLevel the level of the message (debug or regular event)
     * @param termWord the color the message will be printed in, or keyword for operations
     */
    public void report(String msg, int msgLevel, String termWord) {
        if (termWord.equals("set")) {
            tC.simpleColorPrint(msg);
        } else {
            if (msgLevel == 0) {
                tC.colorPrint(termWord, msg);
            } else {
                if (debugLevel == 1) {
                    tC.colorPrint(termWord, msg);
                }
            }
        }
    }

}
