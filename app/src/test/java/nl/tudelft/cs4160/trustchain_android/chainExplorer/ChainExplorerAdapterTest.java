package nl.tudelft.cs4160.trustchain_android.chainExplorer;


import com.google.protobuf.ByteString;

import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ChainExplorerAdapterTest {
    private static final ByteString PEER_2_PEER = ByteString.copyFromUtf8("peer2peer");
    private ChainExplorerAdapter adapter = new ChainExplorerAdapter(null, null, new byte[0]);

    @Test
    public void find_peer_in_list() {
        adapter.peerList = singlePeer();
        String peerString = adapter.findInPeersOrAdd(PEER_2_PEER);

        assertEquals(peerString, "me");
    }

    @Test
    public void add_peer_if_not_in_list() {
        adapter.peerList = singlePeer();
        ByteString newPeer = ByteString.copyFromUtf8("new peer");
        String peerString = adapter.findInPeersOrAdd(newPeer);

        assertEquals(peerString, "peer0");
        assertTrue(adapter.peerList.containsKey(newPeer));
    }

    private static HashMap<ByteString, String> singlePeer() {
        HashMap<ByteString, String> peers = new HashMap<>();
        peers.put(PEER_2_PEER, "me");

        return peers;
    }

    @Test
    public void string_for_known_sequence_number() {
        assertEquals("42", ChainExplorerAdapter.displayStringForSequenceNumber(42));
    }

    @Test
    public void string_for_unknown_sequence_number() {
        assertEquals("unknown", ChainExplorerAdapter.displayStringForSequenceNumber(0));
    }
}
