package serverClient;

import java.io.File;

public class FileThread {

    ClientThread receiverThread;
    File file;

    public FileThread(ClientThread receiverThread, File file) {
        this.receiverThread = receiverThread;
        this.file = file;
    }
}
