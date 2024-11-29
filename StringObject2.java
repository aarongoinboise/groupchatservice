import java.io.Serializable;

public class StringObject2 implements Serializable {
    private String message;

    public StringObject2(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return message;
    }
}
