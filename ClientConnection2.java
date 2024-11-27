import java.io.IOException;
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

    public boolean isOpen(){
        return in == null && out == null;
    }

    public synchronized String getNickname() {
        return this.nickName;
    }

    public synchronized void writeMessage(String message) throws IOException{
        out.writeObject(message);
        out.flush();
    }

    public synchronized String readMessage() throws ClassNotFoundException, IOException  {
        return (String) in.readObject();
    }
}
