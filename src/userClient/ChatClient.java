package userClient;

import javax.net.SocketFactory;
import javax.net.ssl.*;
import java.io.*;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatClient extends Thread {
    public static String RETRY = "RETRY";
    public static String GOOD = "GOOD";
    public static String status = GOOD;
    private TimerThread timerThread;
    private SSLSocket connection;
    private InputThread inputThread;
    private File filetoBeSent;

    public static void main(String[] args) {
        ChatClient chatClient = new ChatClient();
        chatClient.run();
    }

    public void run() {
        //maak connectie met de server
        Scanner scanner = new Scanner(System.in);
        connection = makeConnection();
        try {
            connection.startHandshake();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //start een input thread die luisterd naar berichten van de server
        CopyOnWriteArrayList<String> messageList = new CopyOnWriteArrayList<>();
        inputThread = new InputThread(connection, messageList, this);
        inputThread.start();


        OutputStream outputStream;
        try {
            outputStream = connection.getOutputStream();
            String input = "";
            PrintWriter writer = new PrintWriter(outputStream);

            //zolang de client geen QUIT zegt blijf wachten op input
            while (!input.equalsIgnoreCase("QUIT")) {
                input = scanner.nextLine();

                //als de status is veranderd naar retry maak de connectie opnieuw
                if (status.equals(RETRY)){
                    writer = new PrintWriter(connection.getOutputStream());
                    status = GOOD;
                }
                //als de input niet niks is voeg het bericht dan toe aan de lijst en start een nieuwe thread die kijkt of er
                //wel antwoord komt
                if (!input.equals("")) {
                    InputThread.ServerMessage message = inputThread.parseServerAnswer(InputThread.ServerMessage.UNKNOWN,input);
                    if (message == InputThread.ServerMessage.SFILE){
                        String fileName = input.split(" ")[input.split(" ").length-1];
                        filetoBeSent = new File(new File(fileName).getAbsolutePath());
                        if (filetoBeSent.exists()){
                            writer.println(input);
                            writer.flush();
                        } else {
                            System.out.println("-ERR file does not exist!(dont forget the extension and put it in the right folder!)");
                        }
                    } else {
                        messageList.add(input);
                        timerThread = new TimerThread(input,connection,this);
                        timerThread.start();
                        writer.println(input);
                        writer.flush();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public SSLSocket makeConnection() {
        try {
            //maak een beveiligde connectie tussen server en client door SSLSockets en een certificaat
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
        //maak de connectie opnieuw als de connectie is weg gevallen
        SocketFactory socketFactory = SSLSocketFactory.getDefault();
        connection = (SSLSocket) socketFactory.createSocket("127.0.0.1", 1337);
        connection.startHandshake();
        inputThread.setConnection(connection);
    }

    public void resetTimer(){
        //reset de timer die bijhoud of er wel antwoord van de server komt
        if (timerThread != null) {
            timerThread.setReply();
        }
    }

    public File getFiletoBeSent() {
        return filetoBeSent;
    }
}
