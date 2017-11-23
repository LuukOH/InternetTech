import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;


public class InputThread extends Thread {
    Socket connection;

    public InputThread(Socket connection) {
        this.connection = connection;
    }

    @Override
    public void run() {
        InputStream inputStream = null;
        try {
            inputStream = connection.getInputStream();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
