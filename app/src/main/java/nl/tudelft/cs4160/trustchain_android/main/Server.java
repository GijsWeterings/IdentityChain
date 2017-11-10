package nl.tudelft.cs4160.trustchain_android.main;

import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

import nl.tudelft.cs4160.trustchain_android.Peer;
import nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock;
import nl.tudelft.cs4160.trustchain_android.block.ValidationResult;
import nl.tudelft.cs4160.trustchain_android.database.TrustChainDBHelper;
import nl.tudelft.cs4160.trustchain_android.message.MessageProto;

import static nl.tudelft.cs4160.trustchain_android.Peer.bytesToHex;
import static nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock.GENESIS_SEQ;
import static nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock.UNKNOWN_SEQ;
import static nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock.getBlock;
import static nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock.validate;
import static nl.tudelft.cs4160.trustchain_android.block.ValidationResult.NO_INFO;
import static nl.tudelft.cs4160.trustchain_android.block.ValidationResult.PARTIAL;
import static nl.tudelft.cs4160.trustchain_android.block.ValidationResult.PARTIAL_PREVIOUS;
import static nl.tudelft.cs4160.trustchain_android.database.TrustChainDBHelper.insertInDB;
import static nl.tudelft.cs4160.trustchain_android.main.MainActivity.DEFAULT_PORT;

/**
 * Class is package private to prevent another activity from accessing it and breaking everything
 */
class Server {
    private static final String TAG = "Server";
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

    private class SocketServerThread extends Thread {
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

    private class SocketServerReplyThread extends Thread {
        private Socket hostThreadSocket;
        int cnt;

        SocketServerReplyThread(Socket socket, int c) {
            hostThreadSocket = socket;
            cnt = c;
        }

        /**
         * Replies to the client that sent a message to the server.
         */
        @Override
        public void run() {
            OutputStream outputStream;
            String msgReply = "message #" + cnt + " received";
            responseLog = "";

            try {
                outputStream = hostThreadSocket.getOutputStream();
                PrintStream printStream = new PrintStream(outputStream);
                printStream.print(msgReply);
                printStream.close();

                responseLog += "replied: " + msgReply + "\n";
            } catch (IOException e) {
                e.printStackTrace();
                responseLog += "Something wrong! " + e.toString() + "\n";
            } finally {
                listener.updateLog("\n  Server: " + responseLog);
            }
        }

    }
}
