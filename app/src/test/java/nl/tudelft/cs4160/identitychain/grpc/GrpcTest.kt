package nl.tudelft.cs4160.identitychain.grpc

import com.google.protobuf.ByteString
import io.grpc.Server
import io.grpc.ServerBuilder
import nl.tudelft.cs4160.identitychain.Util.Key
import nl.tudelft.cs4160.identitychain.block.TrustChainBlock.GENESIS_SEQ
import nl.tudelft.cs4160.identitychain.database.TrustChainMemoryStorage
import nl.tudelft.cs4160.identitychain.message.ChainService
import nl.tudelft.cs4160.identitychain.network.PeerItem
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GrpcTest {
    val testServerOne = peerAndServerForPort(8080)
    val testServerTwo = peerAndServerForPort(8081)

    val serverTwoPeerItem = PeerItem("peerBoy", "localhost", 8081)

    @Test
    fun initial_crawl_request_should_return_genesis_block() {
        val blocks: List<ChainService.PeerTrustChainBlock> = testServerOne.server.connectToPeer(serverTwoPeerItem)

        assertEquals(1, blocks.size)
        assertEquals(GENESIS_SEQ, blocks.first().block.sequenceNumber)
        assertTrue(testServerOne.storage.blocks.contains(blocks.first().block))
    }

    @Test
    fun send_half_block_after_crawl() {
        initial_crawl_request_should_return_genesis_block()
        val payload = "YO dude what up!"
        val sendBlockToKnownPeer = testServerOne.server.sendBlockToKnownPeer(serverTwoPeerItem, payload)!!
        assertEquals(sendBlockToKnownPeer.block.transaction.toStringUtf8(), payload)
        //both should have 4 blocks genesis + the new block for both chains (*2)
        assertEquals(4, testServerOne.storage.blocks.size)
        assertEquals(4, testServerTwo.storage.blocks.size)
    }

    fun peerAndServerForPort(port: Int): TestServer {
        val keyPair = Key.createNewKeyPair()
        val me = ChainService.Peer.newBuilder().setHostname("localhost").setPort(port).setPublicKey(ByteString.copyFrom(keyPair.public.encoded)).build()
        val testStorage = TrustChainMemoryStorage(keyPair)
        val chainServiceServer = ChainServiceServer(testStorage, me, keyPair)

        val grpcServer = ServerBuilder.forPort(port).addService(chainServiceServer).build().start()
        return TestServer(chainServiceServer, me, testStorage, grpcServer)
    }

    data class TestServer(val server: ChainServiceServer, val peer: ChainService.Peer, val storage: TrustChainMemoryStorage, val grpcServer: Server)

    @After
    fun destroyServers() {
        testServerOne.grpcServer.shutdownNow()
        testServerTwo.grpcServer.shutdownNow()
    }
}
