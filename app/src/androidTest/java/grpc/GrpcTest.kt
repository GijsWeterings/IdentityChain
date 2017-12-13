package grpc

import android.support.test.runner.AndroidJUnit4
import android.util.Log
import com.google.common.util.concurrent.ListenableFuture
import com.google.protobuf.ByteString
import io.grpc.ManagedChannelBuilder
import io.grpc.ServerBuilder
import io.grpc.stub.StreamObserver
import junit.framework.Assert.fail
import nl.tudelft.cs4160.identitychain.Peer
import nl.tudelft.cs4160.identitychain.block.TrustChainBlock
import nl.tudelft.cs4160.identitychain.block.ValidationResult
import nl.tudelft.cs4160.identitychain.connection.Communication
import nl.tudelft.cs4160.identitychain.database.TrustChainDBHelper
import nl.tudelft.cs4160.identitychain.message.ChainGrpc
import nl.tudelft.cs4160.identitychain.message.ChainService
import nl.tudelft.cs4160.identitychain.message.MessageProto
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyPair
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class GrpcTest {

    @Test
    fun testClientAndServer() {
        //start server
        ServerBuilder.forPort(8080).addService(ChainServiceServer()).build().start()

        val mChannel = ManagedChannelBuilder.forAddress("localhost", 8080).usePlaintext(true).build();
        val stub = ChainGrpc.newBlockingStub(mChannel)

        val request = MessageProto.CrawlRequest.newBuilder().build()
        val blocks: Iterator<MessageProto.TrustChainBlock> = stub.recieveCrawlRequest(request)
    }

    class ChainClientRegistry {
        val registry: ConcurrentHashMap<ChainService.Peer, ChainGrpc.ChainBlockingStub> = ConcurrentHashMap()

        fun findStub(peer: ChainService.Peer) = registry.getOrPut(peer) { channelForPeer(peer) }

        fun channelForPeer(peer: ChainService.Peer): ChainGrpc.ChainBlockingStub {
            val channel = ManagedChannelBuilder.forAddress(peer.hostname, peer.port).usePlaintext(true).build();
            return ChainGrpc.newBlockingStub(channel)
        }
    }

    class ChainServiceServer(val dbHelper: TrustChainDBHelper, val me: ChainService.Peer,
                             val keyPair: KeyPair, val registry: ChainClientRegistry = ChainClientRegistry()) : ChainGrpc.ChainImplBase() {
        val TAG = "Chainservice"

        val myPublicKey = me.publicKey.toByteArray()

        override fun recieveHalfBlock(request: ChainService.PeerTrustChainBlock, responseObserver: StreamObserver<MessageProto.TrustChainBlock>) {
            val peer = request.peer
            val block = request.block
            Log.i(TAG, "Received half block from peer with IP: " + peer.hostname + ":" + peer.port +
                    " and public key: " + Peer.bytesToHex(peer.publicKey.toByteArray()))

            val validation: ValidationResult
            try {
                validation = TrustChainBlock.validate(block, dbHelper)
            } catch (e: Exception) {
                e.printStackTrace()
                return
            }

            Log.i(TAG, "Received block validation result " + validation.toString() + "("
                    + TrustChainBlock.toString(block) + ")")

            if (validation.getStatus() === ValidationResult.ValidationStatus.INVALID) {
                for (error in validation.getErrors()) {
                    Log.e(TAG, "Validation error: " + error)
                }
                return
            } else {
                dbHelper.insertInDB(block)
            }

            val pk = me.publicKey.toByteArray()
            // check if addressed to me and if we did not sign it already, if so: do nothing.
            if (block.linkSequenceNumber != TrustChainBlock.UNKNOWN_SEQ ||
                    !Arrays.equals(block.linkPublicKey.toByteArray(), pk) ||
                    null != dbHelper.getBlock(block.linkPublicKey.toByteArray(),
                            block.linkSequenceNumber)) {
                Log.e(TAG, "Received block not addressed to me or already signed by me.")
                return
            }

            // determine if we should sign the block, if not: do nothing
            if (!Communication.shouldSign(block)) {
                Log.e(TAG, "Will not sign received block.")
                return
            }

            // check if block matches up with its previous block
            // At this point gaps cannot be tolerated. If we detect a gap we send crawl requests to fill
            // the gap and delay the method until the gap is filled.
            // Note that this code does not cover the scenario where we obtain this block indirectly,
            // because the code does nothing with this block after the crawlRequest was received.
            if (validation.getStatus() === ValidationResult.ValidationStatus.PARTIAL_PREVIOUS || validation.getStatus() === ValidationResult.ValidationStatus.PARTIAL ||
                    validation.getStatus() === ValidationResult.ValidationStatus.NO_INFO) {
                Log.e(TAG, "Request block could not be validated sufficiently, requested crawler. " + validation.toString())
                // send a crawl request, requesting the last 5 blocks before the received halfblock (if available) of the peer
                sendCrawlRequest(peer, block.publicKey.toByteArray(), Math.max(TrustChainBlock.GENESIS_SEQ, block.sequenceNumber - 5))
            } else {
                val signBlock = signBlock(peer, block)
                responseObserver.onNext(signBlock)
                responseObserver.onCompleted()
            }
        }

        fun signBlock(peer: ChainService.Peer, linkedBlock: MessageProto.TrustChainBlock): MessageProto.TrustChainBlock? {
            // do nothing if linked block is not addressed to me
            val myPublicKey = me.publicKey.toByteArray()
            if (!Arrays.equals(linkedBlock.linkPublicKey.toByteArray(), myPublicKey)) {
                Log.e(TAG, "signBlock: Linked block not addressed to me.")
                return null
            }
            // do nothing if block is not a request
            if (linkedBlock.linkSequenceNumber != TrustChainBlock.UNKNOWN_SEQ) {
                Log.e(TAG, "signBlock: Block is not a request.")
                return null
            }
            var block = TrustChainBlock.createBlock(null, dbHelper,
                    myPublicKey,
                    linkedBlock, peer.publicKey.toByteArray())

            block = TrustChainBlock.sign(block, keyPair.private)

            val validation: ValidationResult?
            try {
                validation = TrustChainBlock.validate(block, dbHelper)
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }

            Log.i(TAG, "Signed block to " + Peer.bytesToHex(block.linkPublicKey.toByteArray()) +
                    ", validation result: " + validation!!.toString())

            // only send block if validated correctly
            // If you want to test the sending of blocks and don't care whether or not blocks are valid, remove the next check.
            if (validation != null && validation.getStatus() !== ValidationResult.ValidationStatus.PARTIAL_NEXT && validation.getStatus() !== ValidationResult.ValidationStatus.VALID) {
                Log.e(TAG, "Signed block did not validate. Result: " + validation.toString() + ". Errors: "
                        + validation.getErrors().toString())
            } else {
                dbHelper.insertInDB(block)
                return block
            }
            return null
        }

        fun sendCrawlRequest(peer: ChainService.Peer, publicKey: ByteArray, seqNum: Int) {
            var sq = seqNum
            if (seqNum == 0) {
                sq = dbHelper.getBlock(publicKey,
                        dbHelper.getMaxSeqNum(publicKey))?.sequenceNumber ?: TrustChainBlock.GENESIS_SEQ
            }

            if (sq >= 0) {
                sq = Math.max(TrustChainBlock.GENESIS_SEQ, sq)
            }

            Log.i(TAG, "Requesting crawl of node " + Peer.bytesToHex(publicKey) + ":" + sq)

            val crawlRequest = MessageProto.CrawlRequest.newBuilder()
                    .setPublicKey(ByteString.copyFrom(myPublicKey))
                    .setRequestedSequenceNumber(sq)
                    .setLimit(100).build()

            // send the crawl request
            val message = ChainService.PeerCrawlRequest.newBuilder().setPeer(me).setRequest(crawlRequest).build()
            registry.findStub(peer).recieveCrawlRequest(message)
        }

        override fun recieveCrawlRequest(crawlRequest: ChainService.PeerCrawlRequest, responseObserver: StreamObserver<MessageProto.TrustChainBlock>) {
            var sq = crawlRequest.request.requestedSequenceNumber

            Log.i(TAG, "Received crawl crawlRequest")

            // a negative sequence number indicates that the requesting peer wants an offset of blocks
            // starting with the last block
            if (sq < 0) {
                val lastBlock = dbHelper.getLatestBlock(myPublicKey)

                if (lastBlock != null) {
                    sq = Math.max(TrustChainBlock.GENESIS_SEQ, lastBlock.sequenceNumber + sq + 1)
                } else {
                    sq = TrustChainBlock.GENESIS_SEQ
                }
            }

            val blockList = dbHelper.crawl(myPublicKey, sq)

            for (block in blockList) {
                responseObserver.onNext(block)
            }

            Log.i(TAG, "Sent " + blockList.size + " blocks")
        }
    }
}