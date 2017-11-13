package nl.tudelft.cs4160.trustchain_android.connection.network;

import java.security.KeyPair;

import nl.tudelft.cs4160.trustchain_android.Peer;
import nl.tudelft.cs4160.trustchain_android.connection.Communication;
import nl.tudelft.cs4160.trustchain_android.connection.CommunicationListener;
import nl.tudelft.cs4160.trustchain_android.database.TrustChainDBHelper;
import nl.tudelft.cs4160.trustchain_android.message.MessageProto;


/**
 * Class that is responsible for WiFi connection.
 */
public class NetworkCommunication extends Communication {

    public static final int DEFAULT_PORT = 8080;

    private Server server;

    public NetworkCommunication(TrustChainDBHelper dbHelper, KeyPair kp, CommunicationListener listener) {
        super(dbHelper, kp, listener);
    }

    public void sendMessage(Peer peer, MessageProto.Message message) {
        ClientTask task = new ClientTask(
                peer.getIpAddress(),
                peer.getPort(),
                message,
                getListener());
        task.execute();
    }

    @Override
    public void start() {
        server= new Server(this, getListener());
        server.start();
    }

    @Override
    public void stop() {
        //TODO: make it stop listening
    }


    @Override
    public void addNewPublicKey(Peer p) {
        getPeers().put(p.getIpAddress(), p.getPublicKey());
    }


}