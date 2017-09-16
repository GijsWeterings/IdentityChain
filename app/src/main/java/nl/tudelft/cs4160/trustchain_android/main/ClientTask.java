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

/**
 * Class is package private to prevent another activity from accessing it and breaking everything
 */
class ClientTask extends AsyncTask<Void, Void, Void> {
    Activity callingActivity;
    String destinationIP;
    int destinationPort;
    TempBlock message;

    String response = "";

    ClientTask(String ipAddress, int port, TempBlock message, Activity callingActivity){
        this.destinationIP = ipAddress;
        this.destinationPort = port;
        this.message = message;
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
