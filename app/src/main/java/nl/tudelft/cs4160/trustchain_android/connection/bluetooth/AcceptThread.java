package nl.tudelft.cs4160.trustchain_android.connection.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.google.protobuf.InvalidProtocolBufferException;

import java.io.IOException;
import java.util.UUID;

import nl.tudelft.cs4160.trustchain_android.Peer;
import nl.tudelft.cs4160.trustchain_android.connection.Communication;
import nl.tudelft.cs4160.trustchain_android.connection.CommunicationListener;
import nl.tudelft.cs4160.trustchain_android.message.MessageProto;

import static android.provider.Settings.NameValueTable.NAME;

/**
 * Class that waits for a bluetooth message.
 */
public class AcceptThread extends Thread {

    protected final static UUID myUUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");;


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
            tmp = btAdapter.listenUsingRfcommWithServiceRecord(NAME, AcceptThread.myUUID);
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
                            //communication.receivedMessage();
                            MessageProto.Message message= MessageProto.Message.parseDelimitedFrom(socket.getInputStream());
                            if(message == null) {
                                Log.e(TAG, "Message is null");
                                break;
                            }
                            Peer peer = new Peer(socket.getRemoteDevice());
                            listener.updateLog("Received message");
                            communication.receivedMessage(message, peer);
                            Log.i(TAG, "Received message via bluetooth from " + socket.getRemoteDevice().getName());
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
            Log.i(TAG, "Closing bluetooth connection");
            mmServerSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not close the connect socket", e);
        }
    }
}
