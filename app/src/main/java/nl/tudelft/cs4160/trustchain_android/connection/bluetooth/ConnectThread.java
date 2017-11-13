package nl.tudelft.cs4160.trustchain_android.connection.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;


import java.io.IOException;

import nl.tudelft.cs4160.trustchain_android.connection.CommunicationListener;
import nl.tudelft.cs4160.trustchain_android.message.MessageProto;


/**
 * Class that sends a bluetooth message to another peer.
 */
public class ConnectThread extends Thread {

    private final static String TAG = ConnectThread.class.getName();

    private static BluetoothSocket mmSocket;

    private BluetoothAdapter btAdapter;
    private  MessageProto.Message message;
    private CommunicationListener listener;

    private static boolean alreadyConnected = false;

    public ConnectThread(BluetoothAdapter btAdapter, BluetoothDevice device, MessageProto.Message message, CommunicationListener listener) {
        this.btAdapter = btAdapter;
        this.message = message;
        this.listener = listener;

        try {
            // Get a BluetoothSocket to connect with the given BluetoothDevice.
            // MY_UUID is the app's UUID string, also used in the server code.
            if(mmSocket == null || mmSocket.getRemoteDevice() == null ||
                    !mmSocket.getRemoteDevice().getAddress().equals(device.getAddress())) {
                mmSocket = device.createRfcommSocketToServiceRecord(AcceptThread.myUUID);
                alreadyConnected = false;
            } else {
                alreadyConnected = true;
            }

        } catch (IOException e) {
            Log.e(TAG, "Socket's create() method failed", e);
        }
    }

    public void run() {
        if(!alreadyConnected) {
            // Cancel discovery because it otherwise slows down the connection.
            btAdapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
            }
        }

        // The connection attempt succeeded. Perform work associated with
        // the connection in a separate thread.
        try {
            //send the message
            message.writeDelimitedTo(mmSocket.getOutputStream());
            Log.e(TAG, "Send message via bluetooth to " + mmSocket.getRemoteDevice().getName());
        } catch (IOException e) {
            listener.updateLog("Could not send message: " + e.getMessage());
            mmSocket = null;
            e.printStackTrace();
        }
        //manageMyConnectedSocket(mmSocket);
    }

    // Closes the client socket and causes the thread to finish.
    public void cancel() {
        try {
            Log.i(TAG, "Closing connection");
            mmSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not close the client socket", e);
        }
    }
}