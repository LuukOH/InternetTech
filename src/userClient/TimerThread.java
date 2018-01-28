package userClient;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.io.PrintWriter;

public class TimerThread extends Thread {

    //variabelen
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
        //pak de tijd op het moment van het starten van de thread
        long time = System.currentTimeMillis();
        //zolan er nog geen bericht van de server ontvangen is doe het volgende
        while (noReply){
            //pak de tijd dat deze thread al loopt
            long timeTaken = System.currentTimeMillis() - time;
            //als er een kans is dat de server gewoon lang op zich laat wachten stuur het bericht nog een keer
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
                //als het langer duurt dan 10 seconden probeer opnieuw connectie te maken
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
