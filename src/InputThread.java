import java.io.*;
import java.net.Socket;


public class InputThread extends Thread {
    Socket connection;
    boolean quit = false;

    public InputThread(Socket connection) {
        this.connection = connection;
    }

    @Override
    public void run() {
        InputStream inputStream;
        try {
            inputStream = connection.getInputStream();
            String line = "";
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            while (!quit){
                if ((line = reader.readLine()) != null){
                    if (line.equals("+OK Goodbye")){
                        quit = true;
                    }
                    System.out.println(line);
                }
            }
            connection.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
