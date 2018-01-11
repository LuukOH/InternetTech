package userClient;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatClient extends Thread {
    public static String RETRY = "RETRY";
    private static String GOOD = "GOOD";
    public static String status = GOOD;

    public static void main(String[] args) {
        ChatClient chatClient = new ChatClient();
        chatClient.run();
    }

    public void run() {
        Scanner scanner = new Scanner(System.in);
        Socket connection = makeConnection();

        CopyOnWriteArrayList<String> messageList = new CopyOnWriteArrayList<>();
        InputThread inputThread = new InputThread(connection, messageList, this);
        inputThread.start();


        OutputStream outputStream;
        try {
            outputStream = connection.getOutputStream();
            String input = "";
            PrintWriter writer = new PrintWriter(outputStream);

            while (!input.equals("QUIT")) {
                input = scanner.nextLine();

                if (status.equals(RETRY)){
                    writer = new PrintWriter(inputThread.getConnection().getOutputStream());
                    status = GOOD;
                }
                if (!input.equals("")) {
                    messageList.add(input);
                    writer.println(input);
                    writer.flush();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Socket makeConnection() {
        try {
            Socket connection = new Socket("localhost", 1337);
            return connection;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
