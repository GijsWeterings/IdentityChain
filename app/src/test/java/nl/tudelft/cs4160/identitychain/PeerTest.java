package nl.tudelft.cs4160.identitychain;

import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import static org.junit.Assert.*;

public class PeerTest {
    @Test
    public void bytesToHex() {
        byte[] data = {9, 10, 11};
        String hex = Peer.bytesToHex(data);
        System.out.println(hex);
        String spongyCastle = Hex.toHexString(data);
        assertEquals(hex, spongyCastle);
    }

}