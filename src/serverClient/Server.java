package serverClient;

import javax.net.ServerSocketFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import java.io.*;
import java.net.ServerSocket;
import java.security.*;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class Server {

    private ServerSocket serverSocket;
    private Set<ClientThread> threads;
    private ServerConfiguration conf;


    public Server(ServerConfiguration conf) {
        this.conf = conf;
        Data data = Data.getInstance();
        data.setConf(conf);
    }

    /**
     * Runs the server. The server listens for incoming client connections
     * by opening a socket on a specific port.
     */
    public void run() {
        // Create a socket to wait for clients.
        try {
            //SSLServerSocket die voor een encrypted verbinding zorgt met de client samen
            File dir = new File(new File("keystore.test").getAbsolutePath());
            SSLContext context = SSLContext.getInstance("TLS");
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            KeyStore keyStore = KeyStore.getInstance("JKS");

            keyStore.load(new FileInputStream(dir.getPath()), "storepass".toCharArray());
            keyManagerFactory.init(keyStore, "keypass".toCharArray());
            context.init(keyManagerFactory.getKeyManagers(), null, null);

            ServerSocketFactory factory = context.getServerSocketFactory();

            serverSocket = factory.createServerSocket(conf.SERVER_PORT);


            threads = new HashSet<>();
            Data data = Data.getInstance();
            data.setThreads(threads);

            while (true) {
                // Wait for an incoming client-connection request (blocking).
                SSLSocket socket = (SSLSocket) serverSocket.accept();

                // When a new connection has been established, start a new thread.
                ClientThread ct = new ClientThread(socket);
                threads.add(ct);
                new Thread(ct).start();
                System.out.println("Num clients: " + threads.size());

                // Simulate lost connections if configured.
                if(conf.doSimulateConnectionLost()){
                    DropClientThread dct = new DropClientThread(ct);
                    new Thread(dct).start();
                }
            }
        }catch (GeneralSecurityException ge){
            ge.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This thread sleeps for somewhere between 10 tot 20 seconds and then drops the
     * client thread. This is done to simulate a lost in connection.
     */
    private class DropClientThread implements Runnable {
        ClientThread ct;

        DropClientThread(ClientThread ct){
            this.ct = ct;
        }

        public void run() {
            try {
                // Drop a client thread between 10 to 20 seconds.
                int sleep = (10 + new Random().nextInt(10)) * 1000;
                Thread.sleep(sleep);
                ct.kill();
                threads.remove(ct);
                System.out.println("Num clients: " + threads.size());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This inner class is used to handle all communication between the server and a
     * specific client.
     */

}
