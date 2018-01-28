package serverClient;

import sun.misc.IOUtils;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class FileThread extends Thread {

    private ClientThread receiverThread;
    private ClientThread senderThread;
    private String fileName;
    private boolean first = true;
    private static int serverPort = 1337;
    private Socket sendSocket;
    private Socket receiveSocket;

    public FileThread(ClientThread senderThread, ClientThread receiverThread,String filename) {
        this.senderThread = senderThread;
        this.receiverThread = receiverThread;
        this.fileName = filename;
        serverPort++;
    }

    @Override
    public void run() {
        receiverThread.writeToClient("SFILE Incoming file from " + senderThread.getUsername() + " named: " + fileName + " Do you want to accept? (ACCPT/DND)");
        receiverThread.setFileIsBeingRequested(true);
        receiverThread.setFt(this);
        senderThread.writeToClient("+OK awaiting acceptance");


        long time = System.currentTimeMillis();
        while (!receiverThread.isAnsweredFile() && first) {
            long timeTaken = System.currentTimeMillis() - time;
            if (timeTaken > 15000){
                setAnsweredFile(true);
                setFirstFalse();
            }
            //Do nothing
        }
    }

    public void giveAcceptanceOrDenial(){
        if (receiverThread.isFileTransferState()) {
            //todo: convert to string for transfer
            senderThread.writeToClient("SFILE file is send;" + serverPort);
            sendSocket = getSocket();
            byte[] fileInByte = getFileSent(sendSocket);
            serverPort++;
            receiverThread.writeToClient("SFILE file is being received;" + serverPort);
            receiveSocket = getSocket();
            sendFileToReceiver(fileInByte,receiveSocket);
            receiverThread.writeToClient("SFILE receiving done");


            setAnsweredFile(false);
            setFileTransferStateFalse();
        } else {
            senderThread.writeToClient("SFILE file was denied");
            receiverThread.writeToClient("SFILE file was denied");
            setAnsweredFile(false);
        }
        setFirstFalse();
    }

    public byte[] getFileSent(Socket socket){
        int length = 0;
        try {
            InputStream in = socket.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            byte[] buffer = new byte[4096];

            while ((length = in.read(buffer)) != -1) {
                out.write(buffer, 0, length);
            }

            out.flush();
            out.close();
            in.close();
            return out.toByteArray();
        } catch (IOException e){
            e.printStackTrace();
        }
        return null;
    }

    public void sendFileToReceiver(byte[] file,Socket socket){
        int length = 0;
        try {
            OutputStream out = socket.getOutputStream();
            ByteArrayInputStream in = new ByteArrayInputStream(file);

            byte[] buffer = new byte[8000];

            while ((length = in.read(buffer)) != -1) {
                out.write(buffer, 0, length);
            }
            out.flush();
            out.close();
            in.close();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public Socket getSocket(){
        try {
            ServerSocket serverSocket = new ServerSocket(serverPort);
            Socket socket = serverSocket.accept();
            return socket;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setFileTransferStateFalse(){
        receiverThread.setFileTransferState(false);
        senderThread.setFileTransferState(false);
    }

    public void setAnsweredFile(boolean isAnswered){
        receiverThread.setAnsweredFile(isAnswered);
        senderThread.setAnsweredFile(isAnswered);
    }

    public void setFirstFalse(){
        receiverThread.getFt().setFirst(false);
        senderThread.getFt().setFirst(false);
    }

    public void setFirst(boolean first) {
        this.first = first;
    }
}
