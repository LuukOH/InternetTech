package userClient;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;


public class SendAndReceiveFile extends Thread {

    boolean isSending;
    private Socket socket;
    private File file;

    public SendAndReceiveFile(boolean isSending,Socket socket) {
        this.isSending = isSending;
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            if (isSending) {
                sendFile();
            } else {
                receiveFile();
            }
        } catch (IOException ex){
            ex.printStackTrace();
        }

    }

    public void sendFile() throws IOException {
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        FileInputStream fis = new FileInputStream(file);
        byte[] buffer = new byte[4096];
        int len = 0;

        while ((len = fis.read(buffer)) != -1) {
            out.write(buffer,0,len);
        }

        fis.close();
        out.close();
    }

    public void receiveFile() throws IOException {

        InputStream in =  socket.getInputStream();
        //uncomment the one you want to test
        File file = new File("testPhoto.png");
//        File file = new File("testText.txt");
        FileOutputStream fout = new FileOutputStream(file);

        byte[] buffer = new byte[8000];
        int len = 0;
        while ((len = in.read(buffer)) != -1){
            fout.write(buffer,0,len);
        }
        fout.close();
        in.close();
    }

    public void setFile(File file){
        this.file = file;
    }
}
