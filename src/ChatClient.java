import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatClient {
    Socket connection;
    public static String GOOD = "GOOD";
    public static String RETRY = "RETRY";
    public String status = RETRY;

    public static void main(String[] args) {
        try {
            ChatClient chatClient = new ChatClient();
            chatClient.run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() throws IOException {
        Scanner scanner = new Scanner(System.in);
        while (status.equals(RETRY)){
            status = GOOD;
            Socket connection = makeConnection();
            CopyOnWriteArrayList<String> messageList = new CopyOnWriteArrayList<>();
            InputThread inputThread = new InputThread(connection, messageList,this);
            inputThread.start();

            OutputStream outputStream;
            try {
                outputStream = connection.getOutputStream();
                String input = "";
                PrintWriter writer = new PrintWriter(outputStream);

                while (!input.equals("QUIT") && inputThread.isAlive()){
                    if (status.equals(GOOD)){
                        input = scanner.nextLine();
                    }
                    if (!input.equals("")){
                        messageList.add(input);
                        writer.println(input);
                        writer.flush();
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Socket makeConnection() throws IOException {
        connection = new Socket("localhost", 1337);
        return connection;
    }

}
