import java.util.ArrayList;

public class ClientInfo2 {
    private ArrayList<Client> clients;

    public ClientInfo2() {
        clients = new ArrayList<Client>();
    }

    public void addClient() {

    }

    private class Client {
        String nickname;
        String channelName;

        private Client(String nickname, String channelName) {
            this.nickname = nickname;
            this.channelName = channelName;
        }

        private void setNickName(String nickname) {
            this.nickname = nickname;
        }

        private String getNickname(String nickname) {
            return nickname;
        }

        private void setChannelName(String channelName) {
            this.channelName = channelName;
        }

        private String getChannelName(String channelName) {
            return channelName;
        }
    }
}
