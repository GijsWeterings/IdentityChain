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

    public byte[] getPublicKey() {
        return publicKey;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }

    public String toString() {
        String res = "<Peer: [";
        res += publicKey + ":" + port + ",PubKey: " + bytesToHex(publicKey) + "]>";
        return res;
    }

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

}
