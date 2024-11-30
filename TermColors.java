import java.util.HashMap;
import java.util.Map;

public class TermColors {
    private static final String[] colors = {
            "\u001B[30m", "\u001B[31m", "\u001B[32m", "\u001B[33m", "\u001B[34m", "\u001B[35m", "\u001B[36m",
            "\u001B[37m",
    };
    private static final String[] colorWords = {
            "black", "red", "green", "yellow", "blue", "purple", "cyan", "white"
    };
    private static Map<String, String> colorMap = new HashMap<>();
    private static final String reset = "\u001B[0m";

    public TermColors() {
        for (int i = 0; i < colorWords.length; i++) {
            colorMap.put(colorWords[i], colors[i]);
        }
    }

    public void colorPrint(String termWord, String msg) {
        System.out.println(termWordToColor(termWord) + msg + reset);
    }

    private String termWordToColor(String termWord) {
        for (Map.Entry<String, String> entry : colorMap.entrySet()) {
            if (termWord.equals(entry.getKey())) {
                return entry.getValue();
            }
        }
        return reset;
    }
}
