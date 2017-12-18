package nl.tudelft.cs4160.identitychain.grpc

import android.util.Log
import com.google.protobuf.ByteString
import com.zeroknowledgeproof.rangeProof.*
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.stub.StreamObserver
import io.reactivex.Single
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

class ChainServiceServer(val storage: TrustChainStorage, val me: ChainService.Peer,
                         val keyPair: KeyPair,
                         val uiPrompt: (ChainService.PublicSetupResult) -> Single<Boolean>,
                         val private: SetupPrivateResult,
                         val registry: ChainClientRegistry = ChainClientRegistry()) : ChainGrpc.ChainImplBase() {
    val TAG = "Chainservice"

    val myPublicKey = keyPair.public.encoded

    override fun recieveHalfBlock(request: ChainService.PeerTrustChainBlock, responseObserver: StreamObserver<ChainService.PeerTrustChainBlock>) {
        val validation = saveHalfBlock(request) ?: return
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
            val returnTrustChainBlock = addPeerToBlock(signBlock)
            responseObserver.onNext(returnTrustChainBlock)
            responseObserver.onCompleted()
        }
    }

    class GapInChainException : Exception()


    override fun getPublicKey(request: ChainService.Empty, responseObserver: StreamObserver<ChainService.Key>) {
        responseObserver.onNext(createPublicKey())
        responseObserver.onCompleted()
    }

//    override fun createAgeAttestation(request: ChainService.PeerAgeAttestationRequest, responseObserver: StreamObserver<ChainService.PeerAttestationResponse>) {
//        val (setupPublicResult, setupPrivateResult) = RangeProofTrustedParty().generateProof(request.age, 18, 150)
//        val requestor = request.requestor
//
//
//        uiPrompt.subscribe { accepted ->
//            if (accepted) {
//                val attestationBlock = ageAttestationHalfBlock(setupPublicResult, requestor.publicKey.toByteArray()).let(this::addPeerToBlock)
//
//                if (attestationBlock != null) {
//                    val completeBlock = registry.findStub(requestor).recieveHalfBlock(attestationBlock)
//                    saveHalfBlock(completeBlock)
//                    // block creation done return private parameters
//                    responseObserver.onNext(asPeerAttestationReponse(setupPrivateResult))
//                    responseObserver.onCompleted()
//                } else {
//                    responseObserver.onError(AttestationCreationException())
//                }
//            } else {
//                responseObserver.onError(AttestationCreationException())
//            }
//
//        }
//    }
//
//    fun sendAgeAttestationRequest(age: Int, peerItem: PeerItem): ChainService.PeerAttestationResponse {
//        val request = ChainService.PeerAgeAttestationRequest.newBuilder().setRequestor(me).setAge(age).build()
//
//        return registry.findStub(peerItem.asPeerMessage()).createAgeAttestation(request)
//    }
//
//    fun asPeerAttestationReponse(setupPrivateResult: SetupPrivateResult): ChainService.PeerAttestationResponse {
//        val (m1, m2, m3, r1, r2, r3) = setupPrivateResult
//        return ChainService.PeerAttestationResponse.newBuilder()
//                .setM1(m1.asByteString())
//                .setM2(m2.asByteString())
//                .setM3(m3.asByteString())
//                .setR1(r1.asByteString())
//                .setR2(r2.asByteString())
//                .setR3(r3.asByteString())
//                .build()
//    }
//    class AttestationCreationException : Exception()
//
//
//
//    fun ageAttestationHalfBlock(publicResult: SetupPublicResult, peerKey: ByteArray): MessageProto.TrustChainBlock? {
//        return createNewBlock(publicResult.toString().toByteArray(charset("UTF-8")), peerKey)
//    }

    private fun createPublicKey() =
            ChainService.Key.newBuilder().setPublicKey(ByteString.copyFrom(keyPair.public.encoded)).build()

    override fun sendLatestBlocks(request: ChainService.CrawlResponse, responseObserver: StreamObserver<ChainService.Key>) {
        for (trustChainBlock in request.blockList) {
            saveCompleteBlock(request.peer, trustChainBlock)
        }

        responseObserver.onNext(createPublicKey())
        responseObserver.onCompleted()
    }

    override fun answerChallenge(request: ChainService.Challenge, responseObserver: StreamObserver<ChainService.ChallengeReply>) {
        val publicResult: SetupPublicResult? = this.storage.getBlock(request.pubKey.toByteArray(), request.seqNum)?.asSetupPublic()

        if (publicResult == null) {
            responseObserver.onError(NoSuchBlockException())
        } else {
            val interactiveResult = private.answerUniqueChallenge(Challenge(request.s.asBigInt(), request.t.asBigInt()))
            responseObserver.onNext(interactiveResult.asChallengeReply())
            responseObserver.onCompleted()
        }
    }

    fun verifyExistingBlock(peer: ChainService.Peer, seqNum: Int): Boolean = storage.getBlock(peer.publicKey.toByteArray(), seqNum)?.let { verifyNewBlock(peer, it) } ?: false

    fun verifyNewBlock(peer: ChainService.Peer, block: MessageProto.TrustChainBlock): Boolean {
        val publicKey = peer.publicKey.toByteArray()
        val publicResult: SetupPublicResult = block.asSetupPublic() ?: throw IllegalArgumentException("Block did not contain attestation")
        val rangeProofVerifier = RangeProofVerifier(publicResult.N, publicResult.a, publicResult.b)

        val resultOfAllProofs = (0..10).map {
            val challenge = rangeProofVerifier.requestChallenge(publicResult.k1)

            val challengeMessage = ChainService.Challenge.newBuilder()
                    .setPubKey(ByteString.copyFrom(publicKey))
                    .setSeqNum(block.sequenceNumber)
                    .setS(challenge.s.asByteString())
                    .setT(challenge.t.asByteString())
                    .build()

            val challengeReply = registry.findStub(peer).answerChallenge(challengeMessage).asZkp()(challenge.s, challenge.t)
            rangeProofVerifier.interactiveVerify(publicResult, challengeReply)
        }.all { it }

        return resultOfAllProofs
    }


    fun InteractivePublicResult.asChallengeReply() = ChainService.ChallengeReply.newBuilder()
            .setX(this.x.asByteString())
            .setY(this.y.asByteString())
            .setU(this.u.asByteString())
            .setV(this.v.asByteString())
            .build()


    fun MessageProto.TrustChainBlock.asSetupPublic(): SetupPublicResult? {
        try {
            return ChainService.PublicSetupResult.parseFrom(this.transaction).asZkp()
        } catch (e: Exception) {
            return null
        }
    }

    class NoSuchBlockException : Exception()


    fun saveCompleteBlock(request: ChainService.PeerTrustChainBlock): ValidationResult? = saveCompleteBlock(request.peer, request.block)

    fun saveCompleteBlock(peer: ChainService.Peer, block: MessageProto.TrustChainBlock): ValidationResult? {

        Log.i(TAG, "Received half block from peer with IP: " + peer.hostname + ":" + peer.port +
                " and public key: " + Peer.bytesToHex(peer.publicKey.toByteArray()))

        if (block.linkPublicKey != null && block.publicKey != null) {

            val validation: ValidationResult
            try {
                validation = TrustChainBlock.validate(block, storage)
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
                storage.insertInDB(block)
            }
            return validation
        }

        return null
    }

    fun saveHalfBlock(request: ChainService.PeerTrustChainBlock): ValidationResult? = saveHalfBlock(request.peer, request.block)

    fun saveHalfBlock(peer: ChainService.Peer, block: MessageProto.TrustChainBlock): ValidationResult? {
        Log.i(TAG, "Received half block from peer with IP: " + peer.hostname + ":" + peer.port +
                " and public key: " + Peer.bytesToHex(peer.publicKey.toByteArray()))

        val validation: ValidationResult
        try {
            validation = TrustChainBlock.validate(block, storage)
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
            //TODO this might cause duplicate exceptions.
            storage.insertInDB(block)
        }

        val pk = keyPair.public.encoded
        // check if addressed to me and if we did not sign it already, if so: do nothing.
        if (block.linkSequenceNumber != TrustChainBlock.UNKNOWN_SEQ ||
                !Arrays.equals(block.linkPublicKey.toByteArray(), pk) ||
                null != storage.getBlock(block.linkPublicKey.toByteArray(),
                        block.linkSequenceNumber)) {
            Log.e(TAG, "Received block not addressed to me or already signed by me.")
            return null
        }

        // determine if we should sign the block, if not: do nothing
        if (!shouldSign(peer, block).blockingGet()) {
            Log.e(TAG, "Will not sign received block.")
            return null
        }

        return validation
    }

    fun shouldSign(peer: ChainService.Peer, block: MessageProto.TrustChainBlock): Single<Boolean> {
        return try {
            val setupResult = ChainService.PublicSetupResult.parseFrom(block.transaction)
            val isCorrect = verifyNewBlock(peer, block)

            if (isCorrect) {
                uiPrompt(setupResult)
            } else {
                Log.e(TAG, "the proof was faulty not signing that crap")
                Single.just(false)
            }


        } catch (e: Exception) {
            Log.e(TAG, "we were asked to sign a block but it was not an attestation")
            Single.just(false)
        }
    }

    fun crawlPeer(peer: PeerItem): List<ChainService.PeerTrustChainBlock> {
        val connectablePeer = peer.asPeerMessage()
        val crawledBlocks = sendCrawlRequest(connectablePeer, myPublicKey, -5)

        val bufferedCrawledBlocks = crawledBlocks.asSequence().toList()

        for (crawledBlock in bufferedCrawledBlocks) {
            saveCompleteBlock(crawledBlock)
        }

        return bufferedCrawledBlocks
    }

    fun sendBlockToKnownPeer(peer: PeerItem, payload: String): ChainService.PeerTrustChainBlock? =
            sendBlockToKnownPeer(peer, payload.toByteArray(charset("UTF-8")))

    fun sendBlockToKnownPeer(peer: PeerItem, payload: ByteArray): ChainService.PeerTrustChainBlock? {
        val sequenceNumberForCrawl = sequenceNumberForCrawl(-5)
        val crawledBlocks = storage.crawl(myPublicKey, sequenceNumberForCrawl)

        val crawlResponse = ChainService.CrawlResponse.newBuilder().setPeer(me).addAllBlock(crawledBlocks).build()
        val peerMessage = peer.asPeerMessage()
        val peerChannel = registry.channelForPeer(peerMessage)
        val theirPublicKey: ChainService.Key = peerChannel.sendLatestBlocks(crawlResponse)
        val newBlock = createNewBlock(payload, theirPublicKey.publicKey.toByteArray())

        if (newBlock != null) {
            val trustChainBlock = ChainService.PeerTrustChainBlock.newBuilder().setBlock(newBlock).setPeer(me).build()
            val recievedCompleteBlock = peerChannel.recieveHalfBlock(trustChainBlock)
            saveCompleteBlock(recievedCompleteBlock)
            return recievedCompleteBlock
        } else {
            return null
        }
    }

    fun createNewBlock(transaction: ByteArray, peerKey: ByteArray): MessageProto.TrustChainBlock? {
        val newBlock = TrustChainBlock.createBlock(transaction, storage, myPublicKey, null, peerKey)
        val signedBlock = TrustChainBlock.sign(newBlock, keyPair.private)

        val validation: ValidationResult?
        try {
            validation = TrustChainBlock.validate(signedBlock, storage)
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
            storage.insertInDB(signedBlock)
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
        var block = TrustChainBlock.createBlock(null, storage,
                myPublicKey,
                linkedBlock, peer.publicKey.toByteArray())

        block = TrustChainBlock.sign(block, keyPair.private)

        val validation: ValidationResult?
        try {
            validation = TrustChainBlock.validate(block, storage)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

        Log.i(TAG, "Signed block to " + Peer.bytesToHex(block.linkPublicKey.toByteArray()) +
                ", validation result: " + validation!!.toString())

        // only send block if validated correctly
        // If you want to test the sending of blocks and don't care whether or not blocks are valid, remove the next check.
        if (validation.getStatus() !== ValidationResult.ValidationStatus.PARTIAL_NEXT && validation.getStatus() !== ValidationResult.ValidationStatus.VALID) {
            Log.e(TAG, "Signed block did not validate. Result: " + validation.toString() + ". Errors: "
                    + validation.getErrors().toString())
        } else {
            storage.insertInDB(block)
            return block
        }
        return null
    }

    fun sendCrawlRequest(peer: ChainService.Peer, publicKey: ByteArray, seqNum: Int): MutableIterator<ChainService.PeerTrustChainBlock> {
        var sq = seqNum
        if (seqNum == 0) {
            sq = storage.getBlock(publicKey,
                    storage.getMaxSeqNum(publicKey))?.sequenceNumber ?: TrustChainBlock.GENESIS_SEQ
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
        val blockList = storage.crawl(myPublicKey, sq)

        for (block in blockList) {
            val peerTrustChainBlock = addPeerToBlock(block)
            responseObserver.onNext(peerTrustChainBlock)
        }
        responseObserver.onCompleted()

        Log.i(TAG, "Sent " + blockList.size + " blocks")
    }

    private fun addPeerToBlock(block: MessageProto.TrustChainBlock?) =
            ChainService.PeerTrustChainBlock.newBuilder().setPeer(me).setBlock(block).build()

    private fun sequenceNumberForCrawl(sq: Int): Int {
        return if (sq < 0) {
            val lastBlock = storage.getLatestBlock(myPublicKey)

            if (lastBlock != null) {
                Math.max(TrustChainBlock.GENESIS_SEQ, lastBlock.sequenceNumber + sq + 1)
            } else {
                TrustChainBlock.GENESIS_SEQ
            }
        } else {
            sq
        }
    }

    companion object {

        fun createServer(keyPair: KeyPair, port: Int, host: String, dbHelper: TrustChainStorage, uiPrompt: (ChainService.PublicSetupResult) -> Single<Boolean>, privateStuff: SetupPrivateResult): Pair<ChainServiceServer, Server> {
            val me = ChainService.Peer.newBuilder().setHostname(host).setPort(port).setPublicKey(ByteString.copyFrom(keyPair.public.encoded)).build()
            val chainServiceServer = ChainServiceServer(dbHelper, me, keyPair, uiPrompt, privateStuff)

            val grpcServer = ServerBuilder.forPort(port).addService(chainServiceServer).build().start()
            return Pair(chainServiceServer, grpcServer)
        }
    }
}