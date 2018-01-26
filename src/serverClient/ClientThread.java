package serverClient;
import javax.net.ssl.SSLSocket;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.Set;

import static serverClient.ServerState.*;
import static serverClient.ServerState.FINISHED;

public class ClientThread implements Runnable {

    private DataInputStream is;
    private Data data;
    private OutputStream os;
    private SSLSocket socket;
    private ServerState state;
    public boolean fileTransferState = false;
    public boolean answeredFile = false;
    private String username;
    private ServerConfiguration conf;
    private Set<ClientThread> threads;

    public ClientThread(SSLSocket socket) {
        data = Data.getInstance();
        this.state = INIT;
        this.socket = socket;
        this.conf = data.getConf();
        this.threads = data.getThreads();
    }

    public String getUsername() {
        return username;
    }

    public void run() {
        try {
            // Create input and output streams for the socket.
            os = socket.getOutputStream();
            is = new DataInputStream(socket.getInputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            // According to the protocol we should send HELO <welcome message>
            state = CONNECTING;
            String welcomeMessage = "HELO " + conf.WELCOME_MESSAGE;
            writeToClient(welcomeMessage);

            while (!state.equals(FINISHED)) {
                // Wait for message from the client.
                String line = reader.readLine();
                if (line != null) {
                    // Log incoming message for debug purposes.
                    boolean isIncomingMessage = true;
                    logMessage(isIncomingMessage, line);

                    // Parse incoming message.
                    Message message = new Message(line);

                    // Process message.
                    switch (message.getMessageType()) {
                        case HELO:
                            doHELO(message);
                            break;
                        case BCST:
                            doBCST(message);
                            break;
                        case QUIT:
                            // Close connection
                            state = FINISHED;
                            writeToClient("+OK Goodbye");
                            break;
                        case DM:
                            doDM(message);
                            break;
                        case MGRP:
                            makeGroup(message);
                            break;
                        case JGRP:
                            joinGroup(message);
                            break;
                        case LGRP:
                            leaveGroup(message);
                            break;
                        case KICK:
                            doKICK(message);
                            break;
                        case USRS:
                            getUsers();
                            break;
                        case GRPS:
                            getGroups();
                            break;
                        case USRSGRP:
                            getUsersFromGroup(message);
                            break;
                        case BCGRP:
                            doBCSTGroup(message);
                            break;
                        case ACCPT:
                            fileTransferState = true;
                            answeredFile = true;
                            break;
                        case FILE:
                            sendFile(message);
                            break;
                        case DND:
                            answeredFile = true;
                            break;
                        case UNKOWN:
                            // Unkown command has been sent
                            writeToClient("-ERR Unkown command");
                            break;
                    }
                }
            }
            // Remove from the list of client threads and close the socket.
            threads.remove(this);
            socket.close();
        } catch (IOException e) {
            System.out.println("Server Exception: " + e.getMessage());
        }
    }

    private void doHELO(Message message) {
        // Check username format.
        if (!state.equals(CONNECTED)){
            boolean isValidUsername = message.getPayload().matches("[a-zA-Z0-9_]{3,14}");
            if(!isValidUsername) {
                state = FINISHED;
                writeToClient("-ERR username has an invalid format (only characters, numbers and underscores are allowed)");
            } else {
                // Check if user already exists.
                boolean userExists = false;
                for (ClientThread ct : threads) {
                    if (ct != this && message.getPayload().equals(ct.getUsername())) {
                        userExists = true;
                        break;
                    }
                }
                if (userExists) {
                    writeToClient("-ERR user already logged in");
                } else {
                    state = CONNECTED;
                    this.username = message.getPayload();
                    writeToClient("+OK " + getUsername());
                }
            }
        }
    }

    private void sendFile(Message message) {
        String[] payload = message.getPayload().split(" ");
        boolean found = false;
        if (payload.length != 1) {
            writeToClient("-ERR message is not a valid format!");
        } else {
            String receiver = payload[0];
            FileDialog fd = new FileDialog(new JFrame());
            fd.setVisible(true);
            File[] files = fd.getFiles();
            if (files.length > 0) {
                File file = files[0];
                for (ClientThread ct : threads) {
                    if (ct.username.equals(receiver) && !username.equals(receiver)) {
                        ct.writeToClient("Incoming file named: " + file.getName() + ". Do you want to accept? (ACCPT/DND)");
                        found = true;
                        new FileThread(ct, file);
                    }
                }
                if (!found) {
                    writeToClient("-ERR user not found!");
                } else {
                    writeToClient("+OK awaiting acceptance");
                }
            }
        }

    }

    private void doDM(Message message) {
        String[] messageAndReceiver = message.getPayload().split(" ");
        boolean found = false;
        if (messageAndReceiver.length < 2 || messageAndReceiver.length > 2){
            writeToClient("-ERR message is not a valid format!");
        } else {
            String receiver = messageAndReceiver[0];
            String payload = messageAndReceiver[1];

            for (ClientThread ct : threads) {
                if (ct.username.equals(receiver) && !username.equals(receiver)){
                    ct.writeToClient("DM [" + username + "] " + payload);
                    found = true;
                }
            }
            if (!found){
                writeToClient("-ERR user not found!");
            } else {
                writeToClient("+OK");
            }
        }
    }

    private void makeGroup(Message message) {
        boolean failed = oneArgumentCheck(message);
        if (failed){
            writeToClient("-ERR message is not a valid format!");
        } else {
            Group group = doesGroupExist(message);

            if (group == null){
                Group newGroup = new Group(message.getPayload().split(" ")[0],username);
                data.addGroup(newGroup);
                writeToClient("+OK group made");
            } else {
                writeToClient("-ERR Group already exists!");
            }
        }
    }

    private void joinGroup(Message message) {
        boolean failed = oneArgumentCheck(message);
        if (failed){
            writeToClient("-ERR message is not a valid format!");
        } else {
            boolean alreadyIn = false;

            Group group = doesGroupExist(message);

            if (group != null){
                for (String string : group.getUsers()) {
                    if (string.equals(username)) {
                        writeToClient("-ERR you are already in that group!");
                        alreadyIn = true;
                    }
                }

                if (!alreadyIn) {
                    group.addUser(username);
                    writeToClient("+OK you were added in the group!");
                }
            } else {
                writeToClient("-ERR group doesn't exist!");
            }
        }
    }

    private Group doesGroupExist(Message message){
        for (Group group : data.getGroups()) {
            if (group.getName().equals(message.getPayload().split(" ")[0])){
                return group;
            }
        }
        return null;
    }

    private boolean oneArgumentCheck(Message message){
        String[] fullMessage = message.getPayload().split(" ");
        if (fullMessage.length > 1 || fullMessage[0].equals("")){
            return true;
        } else {
            return false;
        }
    }

    private void leaveGroup(Message message) {
        boolean failed = oneArgumentCheck(message);
        if (failed){
            writeToClient("-ERR message is not a valid format!");
        } else {
            boolean in = false;

            Group group = doesGroupExist(message);

            if (group != null){
                for (String string : group.getUsers()) {
                    if (string.equals(username)) {
                        in = true;
                        group.removeUser(username);
                        if (username.equals(group.getOwner())){
                            if (group.getUsers().size() > 0){
                                group.setOwner(group.getUsers().get(0));
                            } else {
                                data.removeGroup(group);
                            }
                        }
                        writeToClient("+OK you are removed from that group!");
                    }
                }
                if (!in) {
                    writeToClient("-ERR you are not in that group!");
                }
            } else {
                writeToClient("-ERR group doesn't exist!");
            }
        }
    }

    private void doKICK(Message message) {
        String[] fullMessage = message.getPayload().split(" ");
        if (fullMessage.length < 2 || fullMessage.length > 2){
            writeToClient("-ERR message is not a valid format!");
        } else {
            boolean userIn = false;

            Group group = doesGroupExist(message);

            if (group != null){
                if (group.getOwner().equals(username)){
                    for (String string: group.getUsers()){
                        if (string.equals(fullMessage[1]) && !username.equals(fullMessage[1])){
                            userIn = true;
                            writeToClient("+OK user is kicked!");
                            group.removeUser(fullMessage[1]);
                            for (ClientThread ct: threads){
                                if (ct.username.equals(string)){
                                    ct.writeToClient("GRPS you have been kicked from group: " + group.getName());
                                }
                            }
                        }
                    }
                    if (!userIn){
                        writeToClient("-ERR The specified user is not in this group! (remember you can't kick yourself as owner)");
                    }
                } else {
                    writeToClient("-ERR you have no right to kick a user in this group!");
                }
            } else {
                writeToClient("-ERR group doesn't exist!");
            }
        }
    }

    private void getUsers() {
        ArrayList userList = new ArrayList();
        System.out.println("All users currently online:");
        for (ClientThread ct : threads) {
            if (ct.state.equals(CONNECTED)){
                userList.add(ct.username);
            }
        }
        writeToClient("USRS " + userList.toString());
    }

    private void getGroups() {
        ArrayList<Group> groups = data.getGroups();
        ArrayList groupNames = new ArrayList();
        if (groups.size() == 0){
            writeToClient("-ERR no groups exist");
        } else {
            for (int i = 0; i < groups.size(); i++) {
                groupNames.add(groups.get(i).getName());
            }
            writeToClient("GRPS " + groupNames.toString());
        }
    }

    private void doBCST(Message message) {
        // Broadcast to other clients.
        for (ClientThread ct : threads) {
            if (ct != this) {
                ct.writeToClient("BCST [" + getUsername() + "] " + message.getPayload());
            }
        }
        writeToClient("+OK");
    }

    private void doBCSTGroup(Message message){
        String[] fullMessage = message.getPayload().split(" ");
        if (fullMessage.length < 2 || fullMessage.length > 2){
            writeToClient("-ERR message is not a valid format!");
        } else {
            Group group = doesGroupExist(message);

            if (group != null){
                for (String useName :group.getUsers()){
                    for (ClientThread ct: threads){
                        if (useName.equals(ct.getUsername())){
                            if (!useName.equals(username)){
                                ct.writeToClient("BCSTGRP [" + group.getName() + "] " + fullMessage[1]);
                            }
                        }
                    }
                }
                writeToClient("+OK");
            } else {
                writeToClient("-ERR group doesn't exist!");
            }
        }
    }

    private void getUsersFromGroup(Message message){
        boolean failed = oneArgumentCheck(message);
        ArrayList userList = new ArrayList();
        if (failed){
            writeToClient("-ERR message is not a valid format!");
        } else {
            Group group = doesGroupExist(message);

            if (group != null){
                for (String username: group.getUsers()){
                    userList.add(username);
                }
                writeToClient("USRSGRP " + userList.toString());
            } else {
                writeToClient("-ERR group doesn't exist!");
            }
        }
    }

    /**
     * An external process can stop the client using this methode.
     */
    public void kill() {
        try {
            // Log connection drop and close the outputstream.
            System.out.println("[DROP CONNECTION] " + getUsername());
            threads.remove(this);
            socket.close();
        } catch(Exception ex) {
            System.out.println("Exception when closing outputstream: " + ex.getMessage());
        }
        state = FINISHED;
    }

    /**
     * Write a message to this client thread.
     * @param message   The message to be sent to the (connected) client.
     */
    private void writeToClient(String message) {
        boolean shouldDropPacket = false;
        boolean shouldCorruptPacket = false;

        // Check if we need to behave badly by dropping some messages.
        if (conf.doSimulateDroppedPackets()) {
            // Randomly select if we are going to drop this message or not.
            int random = new Random().nextInt(6);
            if (random == 0) {
                // Drop message.
                shouldDropPacket = true;
                System.out.println("[DROPPED] " + message);
            }
        }

        // Check if we need to behave badly by corrupting some messages.
        if (conf.doSimulateCorruptedPackets()) {
            // Randomly select if we are going to corrupt this message or not.
            int random = new Random().nextInt(4);
            if (random == 0) {
                // Corrupt message.
                shouldCorruptPacket = true;
            }
        }

        // Do the actual message sending here.
        if (!shouldDropPacket) {
            if (shouldCorruptPacket){
                message = corrupt(message);
                System.out.println("[CORRUPT] " + message);
            }
            PrintWriter writer = new PrintWriter(os);
            writer.println(message);
            writer.flush();

            // Echo the message to the server console for debugging purposes.
            boolean isIncomingMessage = false;
            logMessage(isIncomingMessage, message);
        }
    }

    /**
     * This methods implements a (naive) simulation of a corrupt message by replacing
     * some charaters at random indexes with the charater X.
     * @param message   The message to be corrupted.
     * @return  Returns the message with some charaters replaced with X's.
     */
    private String corrupt(String message) {
        Random random = new Random();
        int x = random.nextInt(4);
        char[] messageChars =  message.toCharArray();

        while (x < messageChars.length) {
            messageChars[x] = 'X';
            x = x + random.nextInt(10);
        }

        return new String(messageChars);
    }

    /**
     * Util method to print (debug) information about the server's incoming and outgoing messages.
     * @param isIncoming    Indicates whether the message was an incoming message. If false then
     *                      an outgoing message is assumed.
     * @param message       The message received or sent.
     */
    private void logMessage(boolean isIncoming, String message) {
        String logMessage;
        String colorCode = conf.CLI_COLOR_OUTGOING;
        String directionString = ">> ";  // Outgoing message.
        if (isIncoming) {
            colorCode = conf.CLI_COLOR_INCOMING;
            directionString = "<< ";     // Incoming message.
        }

        // Add username to log if present.
        // Note when setting up the connection the user is not known.
        if (getUsername() == null) {
            logMessage = directionString + message;
        } else {
            logMessage = directionString + "[" + getUsername() + "] " + message;
        }

        // Log debug messages with or without colors.
        if(conf.isShowColors()){
            System.out.println(colorCode + logMessage + conf.RESET_CLI_COLORS);
        } else {
            System.out.println(logMessage);
        }
    }
}