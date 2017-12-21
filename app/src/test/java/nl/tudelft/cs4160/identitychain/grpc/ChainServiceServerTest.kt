package nl.tudelft.cs4160.identitychain.grpc

import com.google.protobuf.ByteString
import com.google.protobuf.InvalidProtocolBufferException
import com.zeroknowledgeproof.rangeProof.RangeProofTrustedParty
import io.grpc.Server
import io.grpc.ServerBuilder
import io.reactivex.Single
import nl.tudelft.cs4160.identitychain.Util.Key
import nl.tudelft.cs4160.identitychain.block.TrustChainBlock.GENESIS_SEQ
import nl.tudelft.cs4160.identitychain.database.TrustChainMemoryStorage
import nl.tudelft.cs4160.identitychain.message.ChainService
import nl.tudelft.cs4160.identitychain.network.PeerItem
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import org.junit.Assert.assertNotNull

class ChainServiceServerTest {
    val trustedParty = RangeProofTrustedParty()
    val zkp = trustedParty.generateProof(30, 18, 100)

    val testServerOne = peerAndServerForPort(8080)
    val testServerTwo = peerAndServerForPort(8081)

    val serverTwoPeerItem = PeerItem("peerBoy", "localhost", 8081)
    val serverOnePeerItem = PeerItem("peerBoy", "localhost", 8080)

    @Test
    fun initial_crawl_request_should_return_genesis_block() {
        val blocks: List<ChainService.PeerTrustChainBlock> = testServerOne.server.crawlPeer(serverTwoPeerItem)

        assertEquals(1, blocks.size)
        assertEquals(GENESIS_SEQ, blocks.first().block.sequenceNumber)
        assertTrue(testServerOne.storage.blocks.contains(blocks.first().block))
    }

    @Test(expected = Exception::class)
    fun send_random_crap() {
        initial_crawl_request_should_return_genesis_block()
        val payload = "YO dude what up!"
         testServerOne.server.sendBlockToKnownPeer(serverTwoPeerItem, payload)!!
    }

    @Test
    fun create_attestion() {
        val publicPayLoad = zkp.first.asMessage().toByteArray()
        testServerTwo.server.sendBlockToKnownPeer(serverOnePeerItem, publicPayLoad)
        println(testServerOne.storage.blocks[3])
    }

    @Test
    fun test_verify() {
        val publicPayLoad = zkp.first.asMessage().toByteArray()
        testServerTwo.server.sendBlockToKnownPeer(serverOnePeerItem, publicPayLoad)
        val peer = serverTwoPeerItem.asPeerMessage()
        val peerWithKey = ChainService.Peer.newBuilder(peer).setPublicKey(testServerTwo.peer.publicKey).build()
        val verifyProofWith = testServerOne.server.verifyExistingBlock(peerWithKey, 2)
        assertTrue(verifyProofWith.blockingGet())
    }

    @Test
    fun bidirectional_serialize() {
        val publicPayLoad = zkp.first.asMessage().toByteArray()
        val parse = ChainService.PublicSetupResult.parseFrom(publicPayLoad)
        assertNotNull(parse)
    }

    fun peerAndServerForPort(port: Int): TestServer {
        val keyPair = Key.createNewKeyPair()
        val me = ChainService.Peer.newBuilder().setHostname("localhost").setPort(port).setPublicKey(ByteString.copyFrom(keyPair.public.encoded)).build()
        val testStorage = TrustChainMemoryStorage(keyPair)
        val chainServiceServer = ChainServiceServer(testStorage, me, keyPair, { Single.just(true) }, zkp.second)

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
