package userClient;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;


public class InputThread extends Thread {
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
        DND
    }
    SSLSocket connection;
    CopyOnWriteArrayList messageList;
    private ChatClient chatClient;
    boolean quit = false;
    boolean noResponse = false;
    boolean dropRetry = false;
    long time;
    private InputStream inputStream;
    private BufferedReader reader;

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
                    }


                    System.out.println(line);
                }
            }
            connection.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ServerMessage parseServerAnswer(ServerMessage message,String line) throws IOException{
        String parse = line.split(" ")[0];
        parse = parse.replace("+", "");
        parse = parse.replace("-", "");
        try {
            message = ServerMessage.valueOf(parse);
        } catch (IllegalArgumentException test){
            message = ServerMessage.UNKNOWN;
        }

        if (message == ServerMessage.UNKNOWN){
            if (messageList.size() > 0){
                PrintWriter writer = new PrintWriter(connection.getOutputStream());
                writer.println(messageList.get(messageList.size() - 1));
                writer.flush();
            }
        } else {
            if (messageList.size() > 0){
                messageList.remove(messageList.size() - 1);
            }
        }
        return message;
    }

    public void setConnection(SSLSocket connection)throws IOException {
        this.connection = connection;
        inputStream = connection.getInputStream();
        reader = new BufferedReader(new InputStreamReader(inputStream));
    }

    public SSLSocket getConnection(){
        return connection;
    }
}
