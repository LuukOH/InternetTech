import javax.print.DocFlavor;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class OutputThread extends Thread {
    Socket connection;

    public OutputThread(Socket connection) {
        this.connection = connection;
    }

    @Override
    public void run() {
        OutputStream outputStream;
        try {
            outputStream = connection.getOutputStream();
            String input = "";
            Scanner scanner = new Scanner(System.in);
            PrintWriter writer = new PrintWriter(outputStream);

            while (!input.equals("QUIT")){
                input = scanner.nextLine();
                if (!input.equals("")){
                    writer.println(input);
                    writer.flush();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
