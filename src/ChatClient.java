import java.io.IOException;
import java.net.Socket;

public class ChatClient {
    Socket connection;

    public static void main(String[] args) {
        try {
            ChatClient chatClient = new ChatClient();
            Socket connection = chatClient.makeConnection();

            InputThread inputThread = new InputThread(connection);
            OutputThread outputThread = new OutputThread(connection);
            inputThread.start();
            outputThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Socket makeConnection() throws IOException{
        connection = new Socket("localhost",1337);
        return connection;
    }

}
