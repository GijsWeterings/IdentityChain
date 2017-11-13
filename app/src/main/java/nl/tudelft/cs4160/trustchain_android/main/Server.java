package nl.tudelft.cs4160.trustchain_android.main;

import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

import nl.tudelft.cs4160.trustchain_android.Peer;
import nl.tudelft.cs4160.trustchain_android.R;
import nl.tudelft.cs4160.trustchain_android.Util.Key;
import nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock;
import nl.tudelft.cs4160.trustchain_android.block.ValidationResult;
import nl.tudelft.cs4160.trustchain_android.database.TrustChainDBHelper;
import nl.tudelft.cs4160.trustchain_android.message.MessageProto;

import static nl.tudelft.cs4160.trustchain_android.Peer.bytesToHex;
import static nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock.GENESIS_SEQ;
import static nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock.UNKNOWN_SEQ;
import static nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock.pubKeyToString;
import static nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock.validate;
import static nl.tudelft.cs4160.trustchain_android.block.ValidationResult.NO_INFO;
import static nl.tudelft.cs4160.trustchain_android.block.ValidationResult.PARTIAL;
import static nl.tudelft.cs4160.trustchain_android.block.ValidationResult.PARTIAL_PREVIOUS;
import static nl.tudelft.cs4160.trustchain_android.main.MainActivity.DEFAULT_PORT;
import static nl.tudelft.cs4160.trustchain_android.main.MainActivity.shouldSign;

/**
 * Class is package private to prevent another activity from accessing it and breaking everything
 */
class Server {
    private static final String TAG = "Server";
    MainActivity callingActivity;
    ServerSocket serverSocket;
    TextView statusText;

    String messageLog = "";

    public Server(MainActivity callingActivity) {
        this.callingActivity = callingActivity;
    }

    /**
     * Starts the socketServer thread which will listen for incoming messages.
     */
    public void start() {
        Thread socketServerThread = new Thread(new SocketServerThread());
        socketServerThread.start();

        statusText = (TextView) callingActivity.findViewById(R.id.status);
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
                callingActivity.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        statusText.setText("Waiting for messages...");
                    }
                });

                while (true) {
                    messageLog = "";
                    Socket socket = serverSocket.accept();

                    // We have received a message, this could be either a crawl request or a halfblock
                    MessageProto.Message message = MessageProto.Message.parseFrom(socket.getInputStream());

                    MessageProto.TrustChainBlock block = message.getHalfBlock();
                    MessageProto.CrawlRequest crawlRequest = message.getCrawlRequest();

                    // In case we received a halfblock
                    if(block.getPublicKey().size() > 0 && crawlRequest.getPublicKey().size() == 0) {
                        count++;
                        messageLog += "block received from " + socket.getInetAddress() + ":"
                                + socket.getPort() + "\n"
                                + TrustChainBlock.toShortString(block);
                        callingActivity.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                statusText.append("\n  Server: " + messageLog);
                            }
                        });
                        Log.i(TAG, "block received from " + socket.getInetAddress() + ":"
                                + socket.getPort() + "\n"
                                + TrustChainBlock.toShortString(block));

                        SocketServerReplyThread socketServerReplyThread = new SocketServerReplyThread(
                                socket, count);
                        socketServerReplyThread.run();

                        synchronizedReceivedHalfBlock(socket.getInetAddress(), socket.getPort(), block);
                    }

                    // In case we received a crawlrequest
                    if(block.getPublicKey().size() == 0 && crawlRequest.getPublicKey().size() > 0) {
                        count++;
                        messageLog += "crawlrequest received from " + socket.getInetAddress() + ":"
                                + socket.getPort() + "\n"
                                + "Requested public key: " +
                                pubKeyToString(crawlRequest.getPublicKey().toByteArray(),32)
                                + "\n"
                                + "Requested sequence number: " + crawlRequest.getRequestedSequenceNumber();
                        callingActivity.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                statusText.append("\n\nServer: " + messageLog);
                            }
                        });
                        Log.i(TAG,"crawlrequest received from " + socket.getInetAddress() + ":"
                                + socket.getPort() + "\n"
                                + crawlRequest.toString());

                        SocketServerReplyThread socketServerReplyThread = new SocketServerReplyThread(
                                socket, count);
                        socketServerReplyThread.run();

                        callingActivity.receivedCrawlRequest(socket.getInetAddress(),
                                socket.getPort(), crawlRequest);
                    }

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
            try {
                outputStream = hostThreadSocket.getOutputStream();
                PrintStream printStream = new PrintStream(outputStream);
                printStream.print(msgReply);
                printStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * A half block was send to us and received by us. Someone wants this peer to create the other half
     * and send it back. This method handles that 'request'.
     *  - Checks if the block is valid and puts it in the database if not invalid.
     *  - Checks if the block is addressed to me.
     *  - Determines if we should sign the block
     *  - Check if block matches with its previous block, send crawl request if more information is needed
     */
    public void synchronizedReceivedHalfBlock(InetAddress address, int port, MessageProto.TrustChainBlock block) {
        TrustChainDBHelper dbHelper = callingActivity.getDbHelper();
        Peer peer = new Peer(block.getPublicKey().toByteArray(), address.getHostAddress(), DEFAULT_PORT);
        Log.i(TAG, "Received half block from peer with IP: " + peer.getIpAddress() + ":" + peer.getPort() +
            " and public key: " + bytesToHex(peer.getPublicKey()));

        callingActivity.peers.put(peer.getIpAddress(),peer.getPublicKey());

        ValidationResult validation;
        try {
            validation = validate(block,dbHelper);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        Log.i(TAG,"Received block validation result " + validation.toString() + "("
                + TrustChainBlock.toString(block) + ")");

        if(validation.getStatus() == ValidationResult.INVALID) {
            for(String error: validation.getErrors()) {
                Log.e(TAG, "Validation error: " + error);
            }
            return;
        } else {
            dbHelper.insertInDB(block);
        }

        byte[] pk = Key.loadKeys(callingActivity.getApplicationContext()).getPublic().getEncoded();
        // check if addressed to me and if we did not sign it already, if so: do nothing.
        if(block.getLinkSequenceNumber() != UNKNOWN_SEQ ||
                !Arrays.equals(block.getLinkPublicKey().toByteArray(), pk) ||
                null != dbHelper.getBlock(block.getLinkPublicKey().toByteArray(),
                            block.getLinkSequenceNumber())) {
            Log.e(TAG,"Received block not addressed to me or already signed by me.");
            return;
        }

        // determine if we should sign the block, if not: do nothing
        if(!shouldSign(block)) {
            Log.e(TAG,"Will not sign received block.");
            return;
        }

        // check if block matches up with its previous block
        // At this point gaps cannot be tolerated. If we detect a gap we send crawl requests to fill
        // the gap and delay the method until the gap is filled.
        // Note that this code does not cover the scenario where we obtain this block indirectly,
        // because the code does nothing with this block after the crawlRequest was received.
        if(validation.getStatus() == PARTIAL_PREVIOUS || validation.getStatus() == PARTIAL ||
                validation.getStatus() == NO_INFO) {
            Log.e(TAG, "Request block could not be validated sufficiently, requested crawler. " +
                    validation.toString());
            // send a crawl request, requesting the last 5 blocks before the received halfblock (if available) of the peer
            callingActivity.sendCrawlRequest(peer,block.getPublicKey().toByteArray(),Math.max(GENESIS_SEQ,block.getSequenceNumber()-5));
        } else {
            callingActivity.signBlock(peer, block);
        }
    }

}
