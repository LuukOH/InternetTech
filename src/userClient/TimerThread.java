package userClient;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.io.PrintWriter;

public class TimerThread extends Thread {

    private String message;
    private boolean noReply = true;
    private boolean repeat = false;
    private SSLSocket sslSocket;
    private ChatClient chatClient;

    public TimerThread(String message, SSLSocket sslSocket,ChatClient chatClient) {
        this.message = message;
        this.sslSocket = sslSocket;
        this.chatClient = chatClient;
    }

    @Override
    public void run() {
        long time = System.currentTimeMillis();
        while (noReply){
            long timeTaken = System.currentTimeMillis() - time;
            if (timeTaken > 1000 && timeTaken < 10000){
                try {
                    if (!repeat){
                        PrintWriter writer = new PrintWriter(sslSocket.getOutputStream());
                        writer.println(message);
                        writer.flush();
                    }
                    repeat = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (timeTaken > 10000){
                System.out.println("connection failed! trying to reconnect.");
                try {
                    chatClient.refreshConnection();
                    setReply();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void setReply() {
        this.noReply = false;
    }
}
