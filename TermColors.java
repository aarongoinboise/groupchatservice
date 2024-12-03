import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Object that holds color logic to print colored font on the terminal, with some background logic
 */
public class TermColors {
    private static final String[] colors = {
            "\u001B[47m\u001B[30m", "\u001B[31m", "\u001B[32m", "\u001B[33m", "\u001B[34m", "\u001B[35m", "\u001B[36m",
            "\u001B[40m\u001B[37m",
    };
    private static final String[] colorWords = {
            "black", "red", "green", "yellow", "blue", "purple", "cyan", "white"
    };
    private static Map<String, String> colorMap = new HashMap<>();
    private static final String reset = "\u001B[0m";
    private static Random r = new Random();

    /**
     * Constructor: puts the associated word with it's color value
     */
    public TermColors() {
        for (int i = 0; i < colorWords.length; i++) {
            colorMap.put(colorWords[i], colors[i]);
        }
    }

    /**
     * Prints message with color and proper words
     * 
     * @param termWord the color word
     * @param msg the message
     */
    public void colorPrint(String termWord, String msg) {
        System.out.println(termWordToColor(termWord) + msg + reset);
    }

    /**
     * Does a color print before full parsing of message
     * 
     * @param colorMsg the message with color keyword
     */
    public void simpleColorPrint(String colorMsg) {
        int index = colorMsg.indexOf('|');
        String restOfMsg = colorMsg.substring(index + 1);
        colorPrint(colorMsg.substring(0, index), restOfMsg);
    }

    /**
     * Returns the associated color word with the color value
     * 
     * @param termWord the color word
     * @return the associated color value
     */
    private String termWordToColor(String termWord) {
        if (termWord.equals("random")) {
            termWord = randomTermWord();
        }
        for (Map.Entry<String, String> entry : colorMap.entrySet()) {
            if (termWord.equals(entry.getKey())) {
                return entry.getValue();
            }
        }
        return reset;
    }

    /**
     * Creates a list of colors for a channel
     * 
     * @return the randomized list for colors for users in a channel
     */
    public static List<String> channelColors() {
        ArrayList<String> colorsList = new ArrayList<>(List.of(colorWords));
        Collections.shuffle(colorsList);
        return colorsList.subList(0, 4);
    }

    /**
     * @return a random word of a color
     */
    private static String randomTermWord() {
        int poss = r.nextInt(8);
        return colorWords[poss];
    }
}
