package nl.tudelft.cs4160.trustchain_android.connection;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.google.protobuf.InvalidProtocolBufferException;

import java.io.IOException;

import nl.tudelft.cs4160.trustchain_android.Peer;
import nl.tudelft.cs4160.trustchain_android.main.Communication;
import nl.tudelft.cs4160.trustchain_android.main.CommunicationListener;
import nl.tudelft.cs4160.trustchain_android.message.MessageProto;

import static android.provider.Settings.NameValueTable.NAME;

/**
 * Created by rico on 13-11-17.
 */

public class AcceptThread extends Thread {

    private static final String TAG = AcceptThread.class.getName();
    private BluetoothAdapter btAdapter;
    private final BluetoothServerSocket mmServerSocket;
    private Communication communication;
    private CommunicationListener listener;

    public AcceptThread(BluetoothAdapter bluetoothAdapter, Communication communication, CommunicationListener listener) {
        this.btAdapter = bluetoothAdapter;
        this.communication = communication;
        this.listener = listener;
        // Use a temporary object that is later assigned to mmServerSocket
        // because mmServerSocket is final.
        BluetoothServerSocket tmp = null;
        try {
            // MY_UUID is the app's UUID string, also used by the client code.
            tmp = btAdapter.listenUsingRfcommWithServiceRecord(NAME, BluetoothActivity.myUUID);
        } catch (IOException e) {
            Log.e(TAG, "Socket's listen() method failed", e);
        }
        mmServerSocket = tmp;
    }

    public void run() {
        BluetoothSocket socket;
        // Keep listening until exception occurs or a socket is returned.
        while (true) {
            try {
                socket = mmServerSocket.accept();
            } catch (IOException e) {
                Log.e(TAG, "Socket's accept() method failed", e);
                break;
            }
            if (socket != null) {
                // A connection was accepted. Perform work associated with
                try {
                    while(true) {
                        try {
                            Log.e(TAG, "Starting reading via bluetooth from " + socket.getRemoteDevice().getName());
                            //communication.receivedMessage();
                            MessageProto.Message message= MessageProto.Message.parseDelimitedFrom(socket.getInputStream());
                            if(message == null) {
                                Log.e(TAG, "MESSAGE IS NULL");
                                break;
                            }
                            Peer peer = new Peer(socket.getRemoteDevice());
                            listener.updateLog("Received message");
                            communication.receivedMessage(message, peer);
                            Log.e(TAG, "Received message ");
                        } catch(InvalidProtocolBufferException e) {
                            Log.e(TAG, "invalid message received: " + e.getMessage());
                        }
                    }
                    mmServerSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }

    // Closes the connect socket and causes the thread to finish.
    public void cancel() {
        try {
            Log.e(TAG, "CLOSING CONNECTION");
            mmServerSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not close the connect socket", e);
        }
    }
}
