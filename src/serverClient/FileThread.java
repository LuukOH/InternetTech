package serverClient;

import java.io.File;

public class FileThread extends Thread {

    private ClientThread receiverThread;
    private ClientThread senderThread;
    private File file = null;

    public FileThread(ClientThread senderThread, ClientThread receiverThread) {
        this.senderThread = senderThread;
        this.receiverThread = receiverThread;
    }

    public void addFile(String arrayBytes) {
        //todo: convert string to byte array to file
        file = new File("test.txt");
    }

    @Override
    public void run() {
        while (file != null) {
            //Do nothing
        }
        receiverThread.writeToClient("Incoming file from " + senderThread.getUsername() + "named: " + file.getName() + " Do you want to accept? (ACCPT/DND)");
        senderThread.writeToClient("+OK awaiting acceptance");

        while (!receiverThread.answeredFile) {
            //Do nothing
        }
        if (receiverThread.fileTransferState) {
            //todo: convert to string for transfer
            senderThread.writeToClient("+OK file send");
            receiverThread.writeToClient("+OK file received");
            receiverThread.answeredFile = false;
            receiverThread.fileTransferState = false;
        } else {
            senderThread.writeToClient("+OK file was denied");
            receiverThread.writeToClient("+OK file was denied");
            receiverThread.answeredFile = false;
        }
    }
}
