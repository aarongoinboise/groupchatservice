public class Reporter2 {
    private int debugLevel;
    private TermColors tC;

    public Reporter2(int debugLevel) {
        this.debugLevel = debugLevel;
        tC = new TermColors();
    }

    public void report(String msg, int msgLevel) {
        if (msgLevel == 0) {
            tC.colorPrint("green", msg);
        } else {
            if (debugLevel == 1) {
                tC.colorPrint("green", msg);
            }
        }
    }

}
