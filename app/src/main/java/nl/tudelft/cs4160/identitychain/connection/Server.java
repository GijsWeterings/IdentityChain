package nl.tudelft.cs4160.identitychain.connection;


import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

import nl.tudelft.cs4160.identitychain.Peer;
import nl.tudelft.cs4160.identitychain.connection.Communication;
import nl.tudelft.cs4160.identitychain.connection.CommunicationListener;
import nl.tudelft.cs4160.identitychain.message.MessageProto;


/**
 * Class is package private to prevent another activity from accessing it and breaking everything
 */
class Server {
    ServerSocket serverSocket;

    String messageLog = "";
    String responseLog = "";

    private Communication communication;
    private CommunicationListener listener;

    public Server(Communication communication, CommunicationListener listener) {
        this.communication = communication;
        this.listener = listener;
    }

    /**
     * Starts the socketServer thread which will listen for incoming messages.
     */
    public void start() {
        Thread socketServerThread = new Thread(new SocketServerThread());
        socketServerThread.start();

    }

    private class SocketServerThread implements Runnable {
        static final int SocketServerPORT = 8080;
        int count = 0;

        /**
         * Starts the serverSocket, in the while loop it starts listening for messages.
         * serverSocket.accept() blocks until a message is received.
         */
        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(SocketServerPORT);

                listener.updateLog("Server is waiting for messages...");

                while (true) {
                    messageLog = "";
                    Socket socket = serverSocket.accept();

                    // We have received a message, this could be either a crawl request or a halfblock
                    MessageProto.Message message = MessageProto.Message.parseFrom(socket.getInputStream());
                    Peer peer = new Peer(null, socket.getInetAddress().getHostAddress(), socket.getPort());
                    communication.receivedMessage(message, peer);

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}