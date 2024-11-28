// import java.util.Scanner;

// public class ChatClient2 {
//     public static void main(String[] args) {
//         if (args.length != 0) {
//             ChatServerParser2.printUsageAndExit("usage: java ChatClient\nNo arguments.");
//         }
//         try (Scanner commandInput = new Scanner(System.in)) {
//             while (true) {
//                 CommandHelper2.helpMsg();
//                 System.out.print("Enter a command: ");
//                 String fullCmd= commandInput.nextLine();
//                 Scanner cmdScan = new Scanner(fullCmd);
//                 cmdScan.useDelimiter("\\s+");
//                 String cmd = cmdScan.next();
//                 if (cmd.equals("/connect")) {
//                     String serverName = cmdScan.next();
//                     String pString = cmdScan.next();
//                     ChatClientCmder2.connect(serverName, pString);
//                 }
//                 cmdScan.close();
//             }
//         } catch (Exception e) {
//             ChatServerParser2.printUsageAndExit("Refer to the possible commands, and check server names.");
//         }
//     }
// }

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Scanner;

public class ChatClient2 {
    private static final String CONNECT_COMMAND = "/connect ";
    private static final String QUIT_COMMAND = "/quit";
    private static boolean connected = false;

    public static void main(String[] args) {
        if (args.length != 0) {
            ChatServerParser2.printUsageAndExit("usage: java ChatClient\nNo arguments.");
        }

        Scanner scanner = new Scanner(System.in);

        while (true) {
            if (!connected) {
                System.out.println("Type '/connect <host> <port>' to start:");
                String input = scanner.nextLine();
                if (input.startsWith(CONNECT_COMMAND)) {
                    String[] parts = input.substring(CONNECT_COMMAND.length()).split(" ");
                    if (parts.length < 2) {
                        System.out.println("Invalid command. Usage: /connect <host> <port>");
                        continue;
                    }

                    String host = parts[0];
                    int port = Integer.parseInt(parts[1]);
                    try {
                        new ChatClient2().startClient(host, port);
                        connected = true;
                    } catch (IOException e) {
                        System.out.println("Failed to connect: " + e.getMessage());
                    }
                } else {
                    System.out.println("Not a connect command!");
                }
            }
        }
    }

    private void startClient(String host, int port) throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.connect(new InetSocketAddress(host, port));

        Selector selector = Selector.open();
        socketChannel.register(selector, SelectionKey.OP_CONNECT);

        ByteBuffer buffer = ByteBuffer.allocate(1024);
        Scanner scanner = new Scanner(System.in);

        System.out.println("Connecting to server...");
        while (true) {
            selector.select();

            Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                keyIterator.remove();

                if (key.isConnectable()) {
                    SocketChannel channel = (SocketChannel) key.channel();
                    if (channel.finishConnect()) {
                        System.out.println("Connected to server.");
                        channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                    }
                } else if (key.isReadable()) {
                    SocketChannel channel = (SocketChannel) key.channel();
                    buffer.clear();
                    int bytesRead = channel.read(buffer);
                    if (bytesRead == -1) {
                        System.out.println("Disconnected from server.");
                        channel.close();
                        connected = false;
                        return;
                    }
                    buffer.flip();
                    System.out.println("Server: " + new String(buffer.array(), 0, buffer.limit()));
                } else if (key.isWritable()) {
                    SocketChannel channel = (SocketChannel) key.channel();
                    System.out.print("You: ");
                    String message = scanner.nextLine();
                    if (message.equals(QUIT_COMMAND)) {
                        channel.close();
                        System.out.println("Disconnected.");
                        connected = false;
                        return;
                    }
                    buffer.clear();
                    buffer.put(message.getBytes());
                    buffer.flip();
                    channel.write(buffer);
                }
            }
        }
    }
}
