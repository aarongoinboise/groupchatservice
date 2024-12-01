public class Reporter {
    private int debugLevel;
    private TermColors tC;

    public Reporter(int debugLevel) {
        this.debugLevel = debugLevel;
        tC = new TermColors();
    }

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
