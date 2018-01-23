package nl.tudelft.cs4160.identitychain.grpc

import com.google.protobuf.ByteString
import com.zeroknowledgeproof.rangeProof.RangeProofTrustedParty
import com.zeroknowledgeproof.rangeProof.SetupPrivateResult
import io.grpc.Server
import io.grpc.ServerBuilder
import nl.tudelft.cs4160.identitychain.Util.Key
import nl.tudelft.cs4160.identitychain.block.TrustChainBlock.GENESIS_SEQ
import nl.tudelft.cs4160.identitychain.database.FakeRepository
import nl.tudelft.cs4160.identitychain.database.TrustChainMemoryStorage
import nl.tudelft.cs4160.identitychain.message.ChainService
import nl.tudelft.cs4160.identitychain.peers.PeerConnectionInformation
import org.junit.After
import org.junit.Assert.*
import org.junit.Ignore
import org.junit.Test

class ChainServiceServerTest {
    val trustedParty = RangeProofTrustedParty()
    val zkp = trustedParty.generateProof(30, 18, 100)

    val testServerOne = peerAndServerForPort(8080)
    val testServerTwo = peerAndServerForPort(8081)

    val serverOnePeerItem = PeerConnectionInformation( "localhost", 8080)
    val serverTwoPeerItem = PeerConnectionInformation( "localhost", 8081)

    @Test
    fun initial_crawl_request_should_return_genesis_block() {
        val blocks = testServerOne.server.crawlPeer(serverTwoPeerItem).blockingGet().blockList

        assertEquals(1, blocks.size)
        assertEquals(GENESIS_SEQ, blocks.first().sequenceNumber)
        assertTrue(testServerOne.storage.blocks.contains(blocks.first()))
    }

    @Ignore("never finishes again")
    @Test(expected = Exception::class)
    fun send_random_crap() {
        initial_crawl_request_should_return_genesis_block()
        val payload = "YO dude what up!".toByteArray(charset("UTF-8"))
        testServerOne.server.sendBlockToKnownPeer(serverTwoPeerItem, payload, zkp.second).blockingGet()
    }

    @Test
    fun create_attestion_request() {
        val publicPayLoad = zkp.first.asMessage().toByteArray()
        testServerTwo.server.sendBlockToKnownPeer(serverOnePeerItem, publicPayLoad, zkp.second).blockingGet()
        assertTrue(testServerOne.fakeRepository.attestationRequests.any { it.publicKey()?.contentEquals(testServerTwo.peer.publicKey.toByteArray()) ?: false })
        //two genesis block and the newly created half block
        assertTrue(testServerOne.storage.blocks.size == 3)
        //TODO this one misses the genesis block of the other server, maybe a crawl request missing?
        assertTrue(testServerTwo.storage.blocks.size == 2)
    }

    @Test
    fun test_verify() {
        val publicPayLoad = zkp.first.asMessage().toByteArray()
        testServerTwo.server.sendBlockToKnownPeer(serverOnePeerItem, publicPayLoad, zkp.second).blockingGet()
        val peer = testServerTwo.peer
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
        val fakeRepository = FakeRepository()
        val chainServiceServer = ChainServiceServer(testStorage, me, keyPair, fakeRepository)

        val grpcServer = ServerBuilder.forPort(port).addService(chainServiceServer).build().start()
        return TestServer(chainServiceServer, me, testStorage, grpcServer, fakeRepository)
    }

    data class TestServer(val server: ChainServiceServer, val peer: ChainService.Peer, val storage: TrustChainMemoryStorage, val grpcServer: Server, val fakeRepository: FakeRepository)

    @After
    fun destroyServers() {
        testServerOne.grpcServer.shutdownNow()
        testServerTwo.grpcServer.shutdownNow()
    }
}
