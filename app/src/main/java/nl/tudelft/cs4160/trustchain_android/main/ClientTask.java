package nl.tudelft.cs4160.trustchain_android.main;

import android.app.Activity;
import android.os.AsyncTask;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import nl.tudelft.cs4160.trustchain_android.R;
import nl.tudelft.cs4160.trustchain_android.message.MessageProto;

/**
 * Class is package private to prevent another activity from accessing it and breaking everything
 */
class ClientTask extends AsyncTask<Void, Void, Void> {
    Activity callingActivity;
    String destinationIP;
    int destinationPort;
    MessageProto.Message message;

    String response = "";

    ClientTask(String ipAddress, int port, MessageProto.Message message, Activity callingActivity){
        this.destinationIP = ipAddress;
        this.destinationPort = port;
        this.message = message;
        this.callingActivity = callingActivity;
    }

    /**
     * Sends the block or crawlrequest as a message to the specified server (another phone)
     * and listens for a response from the server.
     */
    @Override
    protected Void doInBackground(Void... arg0) {
        Socket socket = null;
        // ugly code cause of statically typed java
        try {
            socket = new Socket(destinationIP, destinationPort);
            message.writeTo(socket.getOutputStream());
            socket.shutdownOutput();

            // Get the response from the server
            ByteArrayOutputStream byteArrayOutputStream =
                    new ByteArrayOutputStream(1024);
            byte[] buffer = new byte[1024];

            int bytesRead;
            InputStream inputStream = socket.getInputStream();

            // notice: inputStream.read() will block if no data return
            while ((bytesRead = inputStream.read(buffer)) != -1){
                byteArrayOutputStream.write(buffer, 0, bytesRead);
                response += byteArrayOutputStream.toString("UTF-8");
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
            response = "UnknownHostException: " + e.toString();
        } catch (IOException e) {
            e.printStackTrace();
            response = "IOException: " + e.toString();
        } catch (Exception e) {
            e.printStackTrace();
            response = "Exception: " + e.toString();
        } finally{
            if(socket != null){
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * After sending a message and receiving a response from the server, update the log.
     */
    @Override
    protected void onPostExecute(Void result) {
        callingActivity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                ((TextView) callingActivity.findViewById(R.id.status)).append("\n  Client: " + response);
            }
        });
        super.onPostExecute(result);
    }

}
