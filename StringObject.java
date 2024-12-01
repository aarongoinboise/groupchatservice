import java.io.Serializable;

public class StringObject implements Serializable {
    private String message;

    public StringObject(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return message;
    }
}
