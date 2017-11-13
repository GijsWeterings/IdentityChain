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
    Activity callingActivity;
    String destinationIP;
    int destinationPort;
    MessageProto.Message message;
    TextView statusText;

    final static String TAG = "ClientTask";
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
        statusText = (TextView) callingActivity.findViewById(R.id.status);
        boolean loop = true;
        while(loop) {
            Socket socket = null;
            try {
                Log.i(TAG, "Opening socket to " + destinationIP + ":" + DEFAULT_PORT);
                socket = new Socket(destinationIP, DEFAULT_PORT);
                message.writeTo(socket.getOutputStream());
                socket.shutdownOutput();

                // check whether we're sending a half block or a message
                if(message.getCrawlRequest().getPublicKey().size() == 0) {
                    Log.i(TAG, "Sent half block to peer with ip " + destinationIP + ":" + destinationPort);
                    callingActivity.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                        statusText.append("\n\nClient: " + "Sent half block to peer with ip " + destinationIP + ":" + destinationPort);
                        }
                    });
                } else {
                    Log.i(TAG, "Sent crawl request to peer with ip " + destinationIP + ":" + destinationPort);
                    callingActivity.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                        statusText.append("\n\nClient: " + "Sent crawl request to peer with ip " + destinationIP + ":" + destinationPort);
                        }
                    });

                }

                // Get the response from the server
                ByteArrayOutputStream byteArrayOutputStream =
                        new ByteArrayOutputStream(1024);
                byte[] buffer = new byte[1024];

                int bytesRead;
                InputStream inputStream = socket.getInputStream();

                // notice: inputStream.read() will block if no data return
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    byteArrayOutputStream.write(buffer, 0, bytesRead);
                    response += byteArrayOutputStream.toString("UTF-8");
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
                callingActivity.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        statusText.append("\n  Client: Cannot resolve host");
                    }
                });
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
        callingActivity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                statusText.append("\n  Client got response: " + response);
            }
        });
        super.onPostExecute(result);
    }

}
