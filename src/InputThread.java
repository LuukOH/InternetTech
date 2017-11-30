import java.io.*;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;


public class InputThread extends Thread {
    public enum ServerMessage {
        OK,BCST,ERR,UNKNOWN,HELO;
    }
    Socket connection;
    CopyOnWriteArrayList messageList;
    private ChatClient chatClient;
    boolean quit = false;
    boolean noResponse = false;
    boolean dropRetry = false;
    long time;

    public InputThread(Socket connection,CopyOnWriteArrayList copyOnWriteArrayList,ChatClient chatClient) {
        this.connection = connection;
        messageList = copyOnWriteArrayList;
        this.chatClient = chatClient;
    }

    @Override
    public void run() {
        InputStream inputStream;
        try {
            inputStream = connection.getInputStream();
            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            while (!quit){
                if (reader.ready()){
                    line = reader.readLine();
                    if (messageList.size() > 0){
                        messageList.remove(messageList.size() - 1);
                        dropRetry = false;
                        noResponse = false;
                    }
                    if (line.equals("+OK Goodbye")){
                        quit = true;
                    }
                    System.out.println(line);
                } else if (messageList.size() > 0){
                    if (noResponse){
                        long timeTaken = System.currentTimeMillis() - time;
                        if (timeTaken > 1000 && timeTaken < 10000){
                            if (!dropRetry) {
                                PrintWriter writer = new PrintWriter(connection.getOutputStream());
                                writer.println(messageList.get(messageList.size() - 1));
                                writer.flush();
                                dropRetry = true;
                            }
                        } else if (timeTaken > 10000){
                            time = System.currentTimeMillis();
                            System.out.println("connection failed! trying to reconnect. Type anything to reconnect");
                            chatClient.status = ChatClient.RETRY;
                            quit = true;
                        }
                    } else {
                        time = System.currentTimeMillis();
                        noResponse = true;
                    }
                }
            }
            connection.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
