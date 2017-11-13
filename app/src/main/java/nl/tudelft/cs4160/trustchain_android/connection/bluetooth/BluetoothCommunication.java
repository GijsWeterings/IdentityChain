package nl.tudelft.cs4160.trustchain_android.connection.bluetooth;

import android.bluetooth.BluetoothAdapter;

import java.security.KeyPair;

import nl.tudelft.cs4160.trustchain_android.Peer;
import nl.tudelft.cs4160.trustchain_android.connection.Communication;
import nl.tudelft.cs4160.trustchain_android.connection.CommunicationListener;
import nl.tudelft.cs4160.trustchain_android.database.TrustChainDBHelper;
import nl.tudelft.cs4160.trustchain_android.message.MessageProto;


/**
 * Class that is responsible for the bluetooth connection.
 */
public class BluetoothCommunication extends Communication {

    private BluetoothAdapter bluetoothAdapter;

    private AcceptThread server;

    public BluetoothCommunication(TrustChainDBHelper dbHelper, KeyPair kp, CommunicationListener listener, BluetoothAdapter bluetoothAdapter) {
        super(dbHelper, kp, listener);
        this.bluetoothAdapter = bluetoothAdapter;

    }
    @Override
    public void sendMessage(Peer peer, MessageProto.Message message) {
        new ConnectThread(bluetoothAdapter, peer.getDevice(), message, getListener()).run();
    }

    @Override
    public void start() {
        server = new AcceptThread(bluetoothAdapter, this, getListener());
        server.start();
    }

    @Override
    public void stop() {
        server.cancel();
    }


    @Override
    public void addNewPublicKey(Peer p) {
        getPeers().put(p.getDevice().getAddress(), p.getPublicKey());
    }
}
