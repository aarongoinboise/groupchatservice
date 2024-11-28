public class Reporter2 {
    private int debugLevel;

    public Reporter2(int debugLevel) {
        this.debugLevel = debugLevel;
    }

    public void report(String msg, int msgLevel) {
        if (msgLevel == 0) {
            System.out.println(msg);
        } else {
            if (debugLevel == 1) {
                System.out.println(msg);
            }
        }
    }

}
