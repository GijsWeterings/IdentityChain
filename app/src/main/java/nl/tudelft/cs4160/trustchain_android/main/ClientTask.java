package nl.tudelft.cs4160.trustchain_android.main;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import nl.tudelft.cs4160.trustchain_android.R;
import nl.tudelft.cs4160.trustchain_android.message.MessageProto;

import static nl.tudelft.cs4160.trustchain_android.main.MainActivity.DEFAULT_PORT;

/**
 * Class is package private to prevent another activity from accessing it and breaking everything
 */
class ClientTask extends AsyncTask<Void, Void, Void> {
    String destinationIP;
    int destinationPort;
    MessageProto.Message message;

    private final static String TAG = ClientTask.class.getName();
    private String response;

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
                Log.i(TAG, "Opening socket to " + destinationIP + ":" + DEFAULT_PORT);
                socket = new Socket(destinationIP, DEFAULT_PORT);
                message.writeTo(socket.getOutputStream());
                socket.shutdownOutput();

                Log.i(TAG, "Sent message to peer with ip " + destinationIP + ":" + destinationPort);

                // Get the response from the server
               /* ByteArrayOutputStream byteArrayOutputStream =
                        new ByteArrayOutputStream(1024);
                byte[] buffer = new byte[1024];

                int bytesRead;
                InputStream inputStream = socket.getInputStream();

                // notice: inputStream.read() will block if no data return
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    byteArrayOutputStream.write(buffer, 0, bytesRead);
                    response += byteArrayOutputStream.toString("UTF-8");
                }*/
            } catch (UnknownHostException e) {
                e.printStackTrace();
                response = "UnknownHostException: " + e.toString();
            } catch (IOException e) {
                e.printStackTrace();
                response = "IOException: " + e.toString();
            } catch (Exception e) {
                e.printStackTrace();
                response = "Exception: " + e.toString();
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
        listener.updateLog("\n  Client got response: " + response);
        super.onPostExecute(result);
    }

}
