import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * A connection to a client which has all relevant information 
 * related to a client for a server
 */
public class ClientConnection{
    public ObjectInputStream in;
    private ObjectOutputStream out;
    private String nickName;
    private boolean connectionOpen;

    /**
     * constructor for ClientConnection
     * 
     * @param in ObjectInputStream connected to the client
     * @param out ObjectOutputStream connected to the client
     * @param nickName client's nickName
     */
    public ClientConnection(ObjectInputStream in, ObjectOutputStream out, String nickName){
        this.connectionOpen = true;
        this.in = in;
        this.out = out;
        this.nickName = nickName;
    }

    /**
     * @return whether or not this ClientConnection is open
     */
    public boolean isOpen(){
        return connectionOpen;
    }

    /**
     * closes the connection to this client
     * 
     * attempts to tell the client before closing the connection
     * 
     * this ClientConnection can no longer be used after calling this method
     * 
     * @throws IOException if the ObjectInputStream or ObjectOutputStream cannot be closed
     */
    public synchronized void closeConnection() throws IOException{
        try {
            if (out != null) {
                out.writeObject(new ChatMessage("server", "disconnecting"));
                out.flush();
            }
        } catch (IOException e) {
            // do nothing
            
        } finally {
            if (in != null) {
                in.close();
              
            }
            if (out != null) {
                out.close();
            }

            in = null;
            out = null;
            connectionOpen = false;
        }
        
    }

    /**
     * @return nickName of client
     */
    public synchronized String getNickname() {
        return this.nickName;
    }

    /**
     * @return true if nickName is null, false if not
     */
    public boolean nicknameIsNull() {
        return nickName == null;
    }

    /**
     * sets this clients nickname and tells the client their new nickname
     * 
     * @param nickName the nickname of this client
     * @throws IOException if there is an issue connecting to the client
     */
    public synchronized void setNickName(String nickName) throws IOException{
        this.nickName = nickName;
    }

    /**
     * send the provided message to this client
     * 
     * @param message
     * @throws IOException if there is an issue connecting to the client
     */
    public synchronized void writeMessage(ChatMessage message) throws IOException{
        out.writeObject(message);
        out.flush();
    }
}