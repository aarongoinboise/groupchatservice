import java.io.Serializable;

/**
 * a chat message 
 */
public class ChatMessage implements Serializable{
    private String sender;
    private String message;

    /**
     * constructor for ChatMessage
     * 
     * @param sender the sender of the message
     * @param messsage the message
     */
    public ChatMessage(String sender, String messsage){
        this.sender = sender;
        this.message = messsage;
    }

    /**
     * constructor for ChatMessage, sets the message sender to null
     * 
     * @param messsage the message
     */
    public ChatMessage(String messsage){
        this.sender = null;
        this.message = messsage;
    }

    /**
     * @return the sender of this message
     */
    public String getSender(){
        return sender;
    }

    /**
     * @return the message being sent
     */
    public String getMessage(){
        return message;
    }
}
