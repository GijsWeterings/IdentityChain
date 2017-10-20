package nl.tudelft.cs4160.trustchain_android;

/**
 * Created by wkmeijer on 20-10-17.
 */

public class Peer {
    byte[] publicKey;
    String ipAddress;
    int port;

    public Peer(byte[] pubKey, String ip, int port) {
        this.publicKey = pubKey;
        this.ipAddress = ip;
        this.port = port;
    }



}
