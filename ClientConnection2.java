import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class ClientConnection2 {
    public ObjectInputStream in;
    private ObjectOutputStream out;
    private String nickName;

    public ClientConnection2(ObjectInputStream in, ObjectOutputStream out) {
        this.in = in;
        this.out = out;
        nickName = null;
    }
}
