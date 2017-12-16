package nl.tudelft.cs4160.identitychain.grpc

import android.util.Log
import com.google.protobuf.ByteString
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.stub.StreamObserver
import nl.tudelft.cs4160.identitychain.Peer
import nl.tudelft.cs4160.identitychain.block.TrustChainBlock
import nl.tudelft.cs4160.identitychain.block.ValidationResult
import nl.tudelft.cs4160.identitychain.database.TrustChainStorage
import nl.tudelft.cs4160.identitychain.message.ChainGrpc
import nl.tudelft.cs4160.identitychain.message.ChainService
import nl.tudelft.cs4160.identitychain.message.MessageProto
import nl.tudelft.cs4160.identitychain.network.PeerItem
import java.security.KeyPair
import java.util.*

class ChainServiceServer(val dbHelper: TrustChainStorage, val me: ChainService.Peer,
                         val keyPair: KeyPair, val registry: ChainClientRegistry = ChainClientRegistry()) : ChainGrpc.ChainImplBase() {
    val TAG = "Chainservice"

    val myPublicKey = keyPair.public.encoded

    override fun recieveHalfBlock(request: ChainService.PeerTrustChainBlock, responseObserver: StreamObserver<ChainService.PeerTrustChainBlock>) {
        val validation = saveBlock(request) ?: return
        val peer = request.peer
        val block = request.block
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
            responseObserver.onError(GapInChainException())
        } else {
            val signBlock = signBlock(peer, block)
            val returnTrustChainBlock = ChainService.PeerTrustChainBlock.newBuilder().setPeer(me).setBlock(signBlock).build()
            responseObserver.onNext(returnTrustChainBlock)
            responseObserver.onCompleted()
        }
    }

    class GapInChainException : Exception()


    override fun getPublicKey(request: ChainService.Empty, responseObserver: StreamObserver<ChainService.Key>) {
        responseObserver.onNext(createPublicKey())
        responseObserver.onCompleted()
    }

    private fun createPublicKey() =
            ChainService.Key.newBuilder().setPublicKey(ByteString.copyFrom(keyPair.public.encoded)).build()

    override fun sendLatestBlocks(request: ChainService.CrawlResponse, responseObserver: StreamObserver<ChainService.Key>) {
        for (trustChainBlock in request.blockList) {
            saveBlock(request.peer, trustChainBlock)
        }

        responseObserver.onNext(createPublicKey())
        responseObserver.onCompleted()
    }

    fun saveBlock(request: ChainService.PeerTrustChainBlock): ValidationResult? = saveBlock(request.peer, request.block)

    fun saveBlock(peer: ChainService.Peer, block: MessageProto.TrustChainBlock): ValidationResult? {
        Log.i(TAG, "Received half block from peer with IP: " + peer.hostname + ":" + peer.port +
                " and public key: " + Peer.bytesToHex(peer.publicKey.toByteArray()))

        val validation: ValidationResult
        try {
            validation = TrustChainBlock.validate(block, dbHelper)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

        Log.i(TAG, "Received block validation result " + validation.toString() + "("
                + TrustChainBlock.toString(block) + ")")

        if (validation.getStatus() === ValidationResult.ValidationStatus.INVALID) {
            for (error in validation.getErrors()) {
                Log.e(TAG, "Validation error: " + error)
            }
            return null
        } else {
            dbHelper.insertInDB(block)
        }

        val pk = keyPair.public.encoded
        // check if addressed to me and if we did not sign it already, if so: do nothing.
        if (block.linkSequenceNumber != TrustChainBlock.UNKNOWN_SEQ ||
                !Arrays.equals(block.linkPublicKey.toByteArray(), pk) ||
                null != dbHelper.getBlock(block.linkPublicKey.toByteArray(),
                        block.linkSequenceNumber)) {
            Log.e(TAG, "Received block not addressed to me or already signed by me.")
            return null
        }

        // determine if we should sign the block, if not: do nothing
        if (!shouldSign(block)) {
            Log.e(TAG, "Will not sign received block.")
            return null
        }

        return validation
    }

    fun connectToPeer(peer: PeerItem): List<ChainService.PeerTrustChainBlock> {
        val connectablePeer = peer.asPeerMessage()
        val crawledBlocks = sendCrawlRequest(connectablePeer, myPublicKey, -5)

        val bufferedCrawledBlocks = crawledBlocks.asSequence().toList()

        for (crawledBlock in bufferedCrawledBlocks) {
            saveBlock(crawledBlock)
        }

        return bufferedCrawledBlocks
    }

    fun sendBlockToKnownPeer(peer: PeerItem, payload: String): ChainService.PeerTrustChainBlock? {
        val sequenceNumberForCrawl = sequenceNumberForCrawl(-5)
        val crawledBlocks = dbHelper.crawl(myPublicKey, sequenceNumberForCrawl)

        val crawlResponse = ChainService.CrawlResponse.newBuilder().setPeer(me).addAllBlock(crawledBlocks).build()
        val peerMessage = peer.asPeerMessage()
        val peerChannel = registry.channelForPeer(peerMessage)
        val theirPublicKey: ChainService.Key = peerChannel.sendLatestBlocks(crawlResponse)
        val newBlock = createNewBlock(payload.toByteArray(charset("UTF-8")), theirPublicKey.publicKey.toByteArray())

        if (newBlock != null) {
            val trustChainBlock = ChainService.PeerTrustChainBlock.newBuilder().setBlock(newBlock).setPeer(me).build()
            val recieveHalfBlock = peerChannel.recieveHalfBlock(trustChainBlock)
            saveBlock(recieveHalfBlock)
            return recieveHalfBlock
        } else {
            return null
        }
    }

    fun createNewBlock(transaction: ByteArray, peerKey: ByteArray): MessageProto.TrustChainBlock? {
        val newBlock = TrustChainBlock.createBlock(transaction, dbHelper, myPublicKey, null, peerKey)
        val signedBlock = TrustChainBlock.sign(newBlock, keyPair.private)

        val validation: ValidationResult?
        try {
            validation = TrustChainBlock.validate(signedBlock, dbHelper)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

        Log.i(TAG, "Signed signedBlock to " + Peer.bytesToHex(signedBlock.linkPublicKey.toByteArray()) +
                ", validation result: " + validation!!.toString())

        // only send signedBlock if validated correctly
        // If you want to test the sending of blocks and don't care whether or not blocks are valid, remove the next check.
        if (validation.getStatus() !== ValidationResult.ValidationStatus.PARTIAL_NEXT && validation.getStatus() !== ValidationResult.ValidationStatus.VALID) {
            Log.e(TAG, "Signed signedBlock did not validate. Result: " + validation.toString() + ". Errors: "
                    + validation.getErrors().toString())
            return null
        } else {
            dbHelper.insertInDB(signedBlock)
            return signedBlock
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

    fun sendCrawlRequest(peer: ChainService.Peer, publicKey: ByteArray, seqNum: Int): MutableIterator<ChainService.PeerTrustChainBlock> {
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
        return registry.findStub(peer).recieveCrawlRequest(message)
    }

    override fun recieveCrawlRequest(crawlRequest: ChainService.PeerCrawlRequest, responseObserver: StreamObserver<ChainService.PeerTrustChainBlock>) {
        val sq = sequenceNumberForCrawl(crawlRequest.request.requestedSequenceNumber)
        println("processing crawl request")
        Log.i(TAG, "Received crawl crawlRequest")

        // a negative sequence number indicates that the requesting peer wants an offset of blocks
        // starting with the last block
        val blockList = dbHelper.crawl(myPublicKey, sq)

        for (block in blockList) {
            val peerTrustChainBlock = ChainService.PeerTrustChainBlock.newBuilder().setPeer(me).setBlock(block).build()
            responseObserver.onNext(peerTrustChainBlock)
        }
        responseObserver.onCompleted()

        Log.i(TAG, "Sent " + blockList.size + " blocks")
    }

    private fun sequenceNumberForCrawl(sq: Int): Int {
        if (sq < 0) {
            val lastBlock = dbHelper.getLatestBlock(myPublicKey)

            if (lastBlock != null) {
                return Math.max(TrustChainBlock.GENESIS_SEQ, lastBlock.sequenceNumber + sq + 1)
            } else {
                return TrustChainBlock.GENESIS_SEQ
            }
        } else {
            return sq
        }
    }

    companion object {
        fun shouldSign(block: MessageProto.TrustChainBlock): Boolean {
            return true
        }

        fun createServer(keyPair: KeyPair, port: Int, host: String, dbHelper: TrustChainStorage): Pair<ChainServiceServer, Server> {
            val me = ChainService.Peer.newBuilder().setHostname(host).setPort(port).setPublicKey(ByteString.copyFrom(keyPair.public.encoded)).build()
            val chainServiceServer = ChainServiceServer(dbHelper, me, keyPair)

            val grpcServer = ServerBuilder.forPort(port).addService(chainServiceServer).build().start()
            return Pair(chainServiceServer, grpcServer)
        }
    }
}