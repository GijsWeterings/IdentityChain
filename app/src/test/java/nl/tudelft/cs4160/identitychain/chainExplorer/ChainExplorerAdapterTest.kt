package nl.tudelft.cs4160.identitychain.chainExplorer


import com.google.protobuf.ByteString

import org.junit.Test

import java.util.HashMap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class ChainExplorerAdapterTest {
    private val adapter = ChainExplorerAdapter(null!!, null!!, ByteArray(0))

    @Test
    fun find_peer_in_list() {
        adapter.peerList = singlePeer()
        val peerString = adapter.findInPeersOrAdd(PEER_2_PEER)

        assertEquals(peerString, "me")
    }

    @Test
    fun add_peer_if_not_in_list() {
        adapter.peerList = singlePeer()
        val newPeer = ByteString.copyFromUtf8("new peer")
        val peerString = adapter.findInPeersOrAdd(newPeer)

        assertEquals(peerString, "peer0")
        assertTrue(adapter.peerList.containsKey(newPeer))
    }

    @Test
    fun string_for_known_sequence_number() {
        assertEquals("42", ChainExplorerAdapter.displayStringForSequenceNumber(42))
    }

    @Test
    fun string_for_unknown_sequence_number() {
        assertEquals("unknown", ChainExplorerAdapter.displayStringForSequenceNumber(0))
    }

    companion object {
        private val PEER_2_PEER = ByteString.copyFromUtf8("peer2peer")

        private fun singlePeer(): HashMap<ByteString, String> {
            val peers = HashMap<ByteString, String>()
            peers.put(PEER_2_PEER, "me")

            return peers
        }
    }
}
