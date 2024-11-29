import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;

/**
 * A channel that clients can join.
 */
public class Channel {
    private String name;
    private boolean executed;
    private Set<ClientConnection> users;

    /**
     * constructor for Channel
     * 
     * @param name name of Channel
     */
    public Channel(String name) {
        this.name = name;
        this.executed = false;
        users = new HashSet<ClientConnection>();
    }

    /**
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the number of clients in this channel
     */
    public int getNumUsers(){
        return users.size();
    }

    /**
     * adds the given client to the channel, client thread is expected to be spun up outside of this class
     * 
     * @param client the client to be added to the channel
     * @return true if the client if successfully added to the channel
     */
    public boolean addClient(ClientConnection client){
        users.add(client);
        return true;
    }

    /**
     * removes the given client from the channel, if present, returns true if the client was in the channel, otherwise false
     * 
     * @param client the client to remove
     */
    public boolean removeClient(ClientConnection client){
        return users.remove(client);
    }

    /**
     * @param client
     * @return true if client is in users, false if not
     */
    public boolean containsClient(ClientConnection client) {
        return users.contains(client);
    }

    /**
     * @return true if run has been executed, false if not (based on boolean value)
     */
    public boolean isExecuted() {
        return executed;
    }

    /**
     * Set when run is executed or thread is stopped
     * @param b
     */
    public void setExecuted(boolean b) {
        this.executed = b;
    }

    /**
     * sends the provided message to every client in the channel
     * 
     * @param message message to be sent
     */
    public void sendMessage(ChatMessage message){
        for(Iterator<ClientConnection> i = users.iterator(); i.hasNext();){
            ClientConnection sendClient = i.next();
            try {
                sendClient.writeMessage(message);
            } catch (IOException e) {
                i.remove();
                try {
                    sendClient.closeConnection();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }


}