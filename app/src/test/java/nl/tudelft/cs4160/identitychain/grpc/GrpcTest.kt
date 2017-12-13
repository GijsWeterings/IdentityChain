package nl.tudelft.cs4160.identitychain.grpc

import io.grpc.ServerBuilder
import nl.tudelft.cs4160.identitychain.Util.Key
import nl.tudelft.cs4160.identitychain.block.TrustChainBlock.GENESIS_SEQ
import nl.tudelft.cs4160.identitychain.database.TrustChainMemoryStorage
import nl.tudelft.cs4160.identitychain.message.ChainService
import nl.tudelft.cs4160.identitychain.network.PeerItem
import org.junit.Assert.assertEquals
import org.junit.Test

class GrpcTest {

    @Test
    fun initial_crawl_request_should_return_genesis_block() {
        val testServerOne = peerAndServerForPort(8080)
        val testServerTwo = peerAndServerForPort(8081)

        val serverTwoPeerItem = PeerItem("peerBoy", "localhost", 8081)
        val blocks = testServerOne.server.connectToPeer(serverTwoPeerItem).asSequence().toList()

        assertEquals(1, blocks.size)
        assertEquals(GENESIS_SEQ, blocks.first().sequenceNumber)
    }

    fun peerAndServerForPort(port: Int): TestServer {
        val me = ChainService.Peer.newBuilder().setHostname("localhost").setPort(port).build()
        val keyPair = Key.createNewKeyPair()
        val testStorage = TrustChainMemoryStorage(keyPair)
        val chainServiceServer = ChainServiceServer(testStorage, me, keyPair)

        ServerBuilder.forPort(port).addService(chainServiceServer).build().start()
        return TestServer(chainServiceServer, me, testStorage)
    }

    data class TestServer(val server: ChainServiceServer, val peer: ChainService.Peer, val storage: TrustChainMemoryStorage)

}