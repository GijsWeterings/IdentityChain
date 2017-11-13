package nl.tudelft.cs4160.trustchain_android.connection.network;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import nl.tudelft.cs4160.trustchain_android.connection.CommunicationListener;
import nl.tudelft.cs4160.trustchain_android.message.MessageProto;


/**
 * Class is package private to prevent another activity from accessing it and breaking everything
 */
class ClientTask extends AsyncTask<Void, Void, Void> {
    String destinationIP;
    int destinationPort;
    MessageProto.Message message;

    private final static String TAG = ClientTask.class.getName();

    private CommunicationListener listener;

    ClientTask(String ipAddress, int port, MessageProto.Message message, CommunicationListener listener){
        this.destinationIP = ipAddress;
        this.destinationPort = port;
        this.message = message;
        this.listener = listener;
    }

    /**
     * Sends the block or crawlrequest as a message to the specified server (another phone)
     * and listens for a response from the server.
     */
    @Override
    protected Void doInBackground(Void... arg0) {
        boolean loop = true;
        while(loop) {
            Socket socket = null;
            try {
                Log.i(TAG, "Opening socket to " + destinationIP + ":" + NetworkCommunication.DEFAULT_PORT);
                socket = new Socket(destinationIP, NetworkCommunication.DEFAULT_PORT);
                message.writeTo(socket.getOutputStream());
                socket.shutdownOutput();

                // check whether we're sending a half block or a message
                if(message.getCrawlRequest().getPublicKey().size() == 0) {
                    Log.i(TAG, "Sent half block to peer with ip " + destinationIP + ":" + destinationPort);
                    listener.updateLog("\n\nClient: " + "Sent half block to peer with ip " + destinationIP + ":" + destinationPort);
                } else {
                    Log.i(TAG, "Sent crawl request to peer with ip " + destinationIP + ":" + destinationPort);
                    listener.updateLog("\n\nClient: " + "Sent crawl request to peer with ip " + destinationIP + ":" + destinationPort);
                }

            } catch (UnknownHostException e) {
                e.printStackTrace();
                listener.updateLog("\n  Client: Cannot resolve host");
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (socket != null) {
                    try {
                        loop = false;
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * After sending a message and receiving a response from the server, update the log.
     */
    @Override
    protected void onPostExecute(Void result) {
        listener.updateLog("\n  Send message ");
        super.onPostExecute(result);
    }

}
