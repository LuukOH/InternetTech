package userClient;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;


public class InputThread extends Thread {
    //all the messages possible
    public enum ServerMessage {
        OK,
        ERR,
        UNKNOWN,
        HELO,
        BCST,
        QUIT,
        USRS,
        DM,
        MGRP,
        GRPS,
        JGRP,
        LGRP,
        KICK,
        ACCPT,
        USRSGRP,
        BCGRP,
        FILE,
        SFILE,
        DND
    }
    //variabelen en objecten
    private SSLSocket connection;
    private Socket receiver;
    private CopyOnWriteArrayList messageList;
    private ChatClient chatClient;
    boolean quit = false;
    boolean dropRetry = false;
    private InputStream inputStream;
    private BufferedReader reader;
    private SendAndReceiveFile fileManager;

    public InputThread(SSLSocket connection,CopyOnWriteArrayList copyOnWriteArrayList,ChatClient chatClient) {
        this.connection = connection;
        messageList = copyOnWriteArrayList;
        this.chatClient = chatClient;
    }

    @Override
    public void run() {

        try {
            inputStream = connection.getInputStream();
            String line;
            reader = new BufferedReader(new InputStreamReader(inputStream));

            while (!quit){
                line = reader.readLine();
                if (line != null && !line.equals("")){
                    chatClient.resetTimer();
                    dropRetry = false;
                    ServerMessage message = ServerMessage.UNKNOWN;



                    message = parseServerAnswer(message,line);

                    if (message.equals(ServerMessage.OK)){
                        String[] splitInput = line.split(" ");
                        if (splitInput[splitInput.length-1].equals("Goodbye")){
                            quit = true;
                        }
                        System.out.println(line);
                    } else if (message.equals(ServerMessage.SFILE) && line.contains("file is send")){
                        //maak een socket aan op de server voor het verzenden van de file
                        int serverPort = Integer.parseInt(line.split(";")[1]);
                        Socket socket = new Socket("localhost",serverPort);
                        System.out.println("SFILE file is being send");
                        fileManager = new SendAndReceiveFile(true,socket);
                        fileManager.setFile(chatClient.getFiletoBeSent());
                        fileManager.start();
                    } else if (message.equals(ServerMessage.SFILE) && line.contains("file is being received")){
                        //maak een socket aan op de server voor het receiven van een file
                        int serverPort = Integer.parseInt(line.split(";")[1]);
                        receiver = new Socket("localhost",serverPort);
                        System.out.println("SFILE file is being received(makes file png by default change by hand sorry!)");
                    } else if (message.equals(ServerMessage.SFILE) && line.contains("receiving done")){
                        fileManager = new SendAndReceiveFile(false,receiver);
                        fileManager.start();
                        System.out.println(line + " (mocht de file niet laden alt-tab 2 keer of sluit de client af(weird bug))");
                    } else{
                        System.out.println(line);
                    }
                }
            }
            connection.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ServerMessage parseServerAnswer(ServerMessage message,String line) throws IOException{
        //pak de message van de server en pak alleen het eerste woord
        String parse = line.split(" ")[0];
        parse = parse.replace("+", "");
        parse = parse.replace("-", "");
        //probeer dat een bekende message te maken als dat niet kan maak er UNKNOWN van
        try {
            message = ServerMessage.valueOf(parse);
        } catch (IllegalArgumentException test){
            message = ServerMessage.UNKNOWN;
        }

        //als de message unknown is stuur het laatste bericht nog een keer, zodat corrupte berichten tegen gegaan kunnen worden
        if (message == ServerMessage.UNKNOWN){
            if (messageList.size() > 0){
                PrintWriter writer = new PrintWriter(connection.getOutputStream());
                writer.println(messageList.get(messageList.size() - 1));
                writer.flush();
            }
        } else {
            //haal de message uit de lijst als deze gewoon netjes is beantwoord
            if (messageList.size() > 0){
                messageList.remove(messageList.size() - 1);
            }
        }
        return message;
    }

    //set de nieuwe connectie als die gedropt is
    public void setConnection(SSLSocket connection)throws IOException {
        this.connection = connection;
        inputStream = connection.getInputStream();
        reader = new BufferedReader(new InputStreamReader(inputStream));
    }
}
