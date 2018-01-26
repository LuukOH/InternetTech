package userClient;

import javax.net.SocketFactory;
import javax.net.ssl.*;
import java.io.*;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatClient extends Thread {
    public static String RETRY = "RETRY";
    private static String GOOD = "GOOD";
    public static String status = GOOD;
    private TimerThread timerThread;
    private SSLSocket connection;
    private InputThread inputThread;

    public static void main(String[] args) {
        ChatClient chatClient = new ChatClient();
        chatClient.run();
    }

    public void run() {
        Scanner scanner = new Scanner(System.in);
        connection = makeConnection();
        try {
            connection.startHandshake();
        } catch (IOException e) {
            e.printStackTrace();
        }

        CopyOnWriteArrayList<String> messageList = new CopyOnWriteArrayList<>();
        inputThread = new InputThread(connection, messageList, this);
        inputThread.start();


        OutputStream outputStream;
        try {
            outputStream = connection.getOutputStream();
            String input = "";
            PrintWriter writer = new PrintWriter(outputStream);

            while (!input.equals("QUIT")) {
                input = scanner.nextLine();

                if (status.equals(RETRY)){
                    writer = new PrintWriter(connection.getOutputStream());
                    status = GOOD;
                }
                if (!input.equals("")) {
                    messageList.add(input);
                    timerThread = new TimerThread(input,connection,this);
                    timerThread.start();
                    writer.println(input);
                    writer.flush();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public SSLSocket makeConnection() {
        try {
            File dir = new File(new File("truststore.test").getAbsolutePath());
            System.setProperty("javax.net.ssl.trustStore", dir.getPath());
            System.setProperty("javax.net.ssl.trustStorePassword", "kyst19999");
            SocketFactory factory = SSLSocketFactory.getDefault();
            SSLSocket socket = (SSLSocket) factory.createSocket("localhost",1337);

            return socket;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void refreshConnection()throws IOException{
        SocketFactory socketFactory = SSLSocketFactory.getDefault();
        connection = (SSLSocket) socketFactory.createSocket("127.0.0.1", 1337);
        connection.startHandshake();
        inputThread.setConnection(connection);
    }

    public void resetTimer(){
        if (timerThread != null) {
            timerThread.setReply();
        }
    }
}
