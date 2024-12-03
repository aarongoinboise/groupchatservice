import java.io.Serializable;

/**
 * Serialized object to send through streams
 */
public class StringObject implements Serializable {
    private String message;

    /*
     * Constructor: initializes message
     */
    public StringObject(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return message;
    }
}
