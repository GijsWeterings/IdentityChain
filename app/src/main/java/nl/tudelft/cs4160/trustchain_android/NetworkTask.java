package nl.tudelft.cs4160.trustchain_android;

import android.app.Activity;
import android.os.AsyncTask;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by meijer on 13-9-17.
 */

public class NetworkTask extends AsyncTask<Void, Void, Void> {
    Activity callingActivity;
    String destinationIP;
    int destinationPort;
    String response = "";

    NetworkTask(String ipAddress, int port, Activity callingActivity){
        this.destinationIP = ipAddress;
        this.destinationPort = port;
        this.callingActivity = callingActivity;
    }

    @Override
    protected Void doInBackground(Void... arg0) {
        Socket socket = null;
        try {
            socket = new Socket(destinationIP, destinationPort);

            ByteArrayOutputStream byteArrayOutputStream =
                    new ByteArrayOutputStream(1024);
            byte[] buffer = new byte[1024];

            int bytesRead;
            InputStream inputStream = socket.getInputStream();

    /*
     * notice:
     * inputStream.read() will block if no data return
     */
            while ((bytesRead = inputStream.read(buffer)) != -1){
                byteArrayOutputStream.write(buffer, 0, bytesRead);
                response += byteArrayOutputStream.toString("UTF-8");
            }

        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            response = "UnknownHostException: " + e.toString();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            response = "IOException: " + e.toString();
        }finally{
            if(socket != null){
                try {
                    socket.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        callingActivity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                ((TextView) callingActivity.findViewById(R.id.messages)).setText(response);
            }
        });
        super.onPostExecute(result);
    }

}
