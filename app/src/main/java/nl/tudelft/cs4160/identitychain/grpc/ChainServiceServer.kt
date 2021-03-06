package nl.tudelft.cs4160.identitychain.grpc

import android.util.Log
import com.google.protobuf.ByteString
import com.zeroknowledgeproof.rangeProof.Challenge
import com.zeroknowledgeproof.rangeProof.RangeProofVerifier
import com.zeroknowledgeproof.rangeProof.SetupPrivateResult
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.stub.StreamObserver
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import nl.tudelft.cs4160.identitychain.Peer
import nl.tudelft.cs4160.identitychain.block.TrustChainBlock
import nl.tudelft.cs4160.identitychain.block.ValidationResult
import nl.tudelft.cs4160.identitychain.database.AttestationRequestRepository
import nl.tudelft.cs4160.identitychain.database.TrustChainStorage
import nl.tudelft.cs4160.identitychain.message.ChainGrpc
import nl.tudelft.cs4160.identitychain.message.ChainService
import nl.tudelft.cs4160.identitychain.message.MessageProto
import nl.tudelft.cs4160.identitychain.peers.*
import java.security.KeyPair
import java.util.*

class ChainServiceServer(val storage: TrustChainStorage, val me: ChainService.Peer,
                         keyPair: KeyPair,
                         val attestationRequestRepository: AttestationRequestRepository,
                         val registry: ChainClientRegistry = ChainClientRegistry()) : ChainGrpc.ChainImplBase() {
    val TAG = "Chainservice"
    val myPublicKey = me.publicKey.toByteArray()
    val signerValidator = BlockSignerValidator(storage, keyPair)
    val empty = ChainService.Empty.getDefaultInstance()

    override fun sendAttestationRequest(request: ChainService.PeerTrustChainBlock, responseObserver: StreamObserver<ChainService.Empty>) {
        val validation = saveHalfBlock(request)
        if (validation == null) {
            //TODO make a better exception for this
            responseObserver.onError(GapInChainException())
            return
        }

        val peer = request.peer.toPeerConnectionInformation()
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
            //TODO get rid of this blocking get
            sendCrawlRequest(peer, block.publicKey.toByteArray(), Math.max(TrustChainBlock.GENESIS_SEQ, block.sequenceNumber - 5)).blockingGet()
            responseObserver.onError(GapInChainException())
        } else {
            attestationRequestRepository.saveAttestationRequest(request)

            responseObserver.onNext(empty)
            responseObserver.onCompleted()
        }
    }

    class GapInChainException : Exception()

    fun keyForPeer(peer: DiscoveredPeer): Single<KeyedPeer> = this.registry.findStub(peer.connectionInformation)
            .getPublicKey(empty).guavaAsSingle(Schedulers.computation())
            .map { addKeyToPeer(peer, it.publicKey.toByteArray()) }
            .onErrorResumeNext { Single.never<KeyedPeer>() }


    override fun getPublicKey(request: ChainService.Empty, responseObserver: StreamObserver<ChainService.Key>) {
        responseObserver.onNext(signerValidator.createPublicKey())
        responseObserver.onCompleted()
    }

    override fun sendLatestBlocks(request: ChainService.CrawlResponse, responseObserver: StreamObserver<ChainService.Key>) {
        for (trustChainBlock in request.blockList) {
            signerValidator.saveCompleteBlock(request.peer, trustChainBlock)
        }

        responseObserver.onNext(signerValidator.createPublicKey())
        responseObserver.onCompleted()
    }

    override fun answerChallenge(request: ChainService.Challenge, responseObserver: StreamObserver<ChainService.ChallengeReply>) {
        if (attestationRequestRepository.getAccess(request.accessPubKey.toByteArray())) {

            val publicResult: ChainService.MetaZkp? = this.storage.getBlock(request.pubKey.toByteArray(), request.seqNum)?.asMetaZkp()
            val private = attestationRequestRepository.privateDataForStuff(request.seqNum)?.toPrivateResult()

            if (publicResult == null || private == null) {
                responseObserver.onError(NoSuchBlockException())
            } else {
                val interactiveResult = private.answerUniqueChallenge(Challenge(request.s.asBigInt(), request.t.asBigInt()))
                responseObserver.onNext(interactiveResult.asChallengeReply())
                responseObserver.onCompleted()
            }
        } else {
            responseObserver.onError(RuntimeException("no access"))
        }
    }

    fun verifyExistingBlock(peer: ChainService.Peer, seqNum: Int): Single<Boolean> = storage.getBlock(peer.publicKey.toByteArray(), seqNum)?.let { verifyNewBlock(peer, it) }
            ?: Single.just(false)

    fun verifyNewBlock(peer: ChainService.Peer, block: MessageProto.TrustChainBlock): Single<Boolean> {
        val publicKey = peer.publicKey.toByteArray()
        val metaZkp: ChainService.MetaZkp = block.asMetaZkp()
        val publicResult = metaZkp.zkp.asZkp()
        val rangeProofVerifier = RangeProofVerifier(publicResult.N, publicResult.a, publicResult.b)

        val resultOfAllProofs: List<Single<Boolean>> = (0..10).map {
            val challenge = rangeProofVerifier.requestChallenge(publicResult.k1)

            val challengeMessage = ChainService.Challenge.newBuilder()
                    .setPubKey(ByteString.copyFrom(publicKey))
                    .setSeqNum(block.sequenceNumber)
                    .setS(challenge.s.asByteString())
                    .setT(challenge.t.asByteString())
                    .setAccessPubKey(me.publicKey)
                    .build()

            registry.findStub(peer.toPeerConnectionInformation()).answerChallenge(challengeMessage).guavaAsSingle(Schedulers.io()).map {
                val challengeReply = it.asZkp()(challenge.s, challenge.t)
                rangeProofVerifier.interactiveVerify(publicResult, challengeReply)
            }.onErrorReturn { false }
        }

        return Flowable.fromIterable(resultOfAllProofs).flatMapSingle { it }.all { it }
    }

    class NoSuchBlockException : Exception()

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
            Log.i(TAG, "saving half block maybe duplicate")
        }

        // check if addressed to me and if we did not sign it already, if so: do nothing.
        if (block.linkSequenceNumber != TrustChainBlock.UNKNOWN_SEQ ||
                !Arrays.equals(block.linkPublicKey.toByteArray(), myPublicKey) ||
                null != storage.getBlock(block.linkPublicKey.toByteArray(),
                        block.linkSequenceNumber)) {
            Log.e(TAG, "Received block not addressed to me or already signed by me.")
            return null
        }

        return validation
    }

    override fun sendSignedBlock(request: ChainService.PeerTrustChainBlock, responseObserver: StreamObserver<ChainService.Empty>) {
        saveHalfBlock(request)

        responseObserver.onNext(empty)
        responseObserver.onCompleted()
    }

    fun signAttestationRequest(peer: ChainService.Peer, block: MessageProto.TrustChainBlock): Single<Boolean> {
        return verifyNewBlock(peer, block).flatMap { zkpValid ->
            if (zkpValid) {
                Log.i(TAG, "verification successful")
                signerValidator.signBlock(peer, block)?.let {
                    registry.findStub(peer.toPeerConnectionInformation()).sendSignedBlock(addPeerToBlock(it)).guavaAsSingle(Schedulers.computation()).map { zkpValid }
                } ?: Single.error<Boolean>(RuntimeException("problems signing the block"))
            } else {
                Log.i(TAG, "verification failed")
                Single.error<Boolean>(RuntimeException("Attestation contained a fake proof"))
            }
        }
    }

    fun crawlPeer(peer: PeerConnectionInformation): Single<ChainService.CrawlResponse> {
        return sendCrawlRequest(peer, myPublicKey, -5)
    }


    fun sendBlockToKnownPeer(peer: PeerConnectionInformation, payload: ByteArray, privateResult: SetupPrivateResult): Single<ChainService.Empty> {
        val sequenceNumberForCrawl = sequenceNumberForCrawl(-5)
        val crawledBlocks = storage.crawl(myPublicKey, sequenceNumberForCrawl)

        val crawlResponse = ChainService.CrawlResponse.newBuilder().setPeer(me).addAllBlock(crawledBlocks).build()
        val peerChannel = registry.findStub(peer)
        val keySingle: Single<ChainService.Key> = startNetworkOnComputation { peerChannel.sendLatestBlocks(crawlResponse).guavaAsSingle(Schedulers.computation()) }

        return keySingle.flatMap { theirPublicKey ->
            val newBlock = signerValidator.createNewBlock(payload, theirPublicKey.publicKey.toByteArray())

            if (newBlock != null) {
                attestationRequestRepository.savePrivateData(privateResult, newBlock.sequenceNumber)
                val trustChainBlock = ChainService.PeerTrustChainBlock.newBuilder().setBlock(newBlock).setPeer(me).build()
                val recievedCompleteBlock: Single<ChainService.Empty> = peerChannel.sendAttestationRequest(trustChainBlock).guavaAsSingle(Schedulers.computation())
                recievedCompleteBlock
            } else {
                Single.error(RuntimeException("could not create new block"))
            }
        }
    }

    fun sendCrawlRequest(peer: PeerConnectionInformation, publicKey: ByteArray, seqNum: Int): Single<ChainService.CrawlResponse> {
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
        //TODO find a better spot for this.
        return registry.findStub(peer).sendCrawlRequest(message).guavaAsSingle(Schedulers.computation())
                .doOnSuccess { crawlResponse -> crawlResponse.blockList.forEach { signerValidator.saveCompleteBlock(crawlResponse.peer, it) } }
    }

    override fun sendCrawlRequest(crawlRequest: ChainService.PeerCrawlRequest, responseObserver: StreamObserver<ChainService.CrawlResponse>) {
        val sq = sequenceNumberForCrawl(crawlRequest.request.requestedSequenceNumber)
        println("processing crawl request")
        Log.i(TAG, "Received crawl crawlRequest")

        // a negative sequence number indicates that the requesting peer wants an offset of blocks
        // starting with the last block
        val blockList = storage.crawl(myPublicKey, sq)
        val response = ChainService.CrawlResponse.newBuilder().addAllBlock(blockList).setPeer(me).build()
        responseObserver.onNext(response)
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

        fun createServer(keyPair: KeyPair, port: Int, host: String, dbHelper: TrustChainStorage,
                         attestationRequestRepository: AttestationRequestRepository): Pair<ChainServiceServer, Server> {
            val me = ChainService.Peer.newBuilder().setHostname(host).setPort(port).setPublicKey(ByteString.copyFrom(keyPair.public.encoded)).build()
            val chainServiceServer = ChainServiceServer(dbHelper, me, keyPair, attestationRequestRepository)

            val grpcServer = ServerBuilder.forPort(port).addService(chainServiceServer).build().start()
            return Pair(chainServiceServer, grpcServer)
        }
    }
}

fun MessageProto.TrustChainBlock.asMetaZkp(): ChainService.MetaZkp {
    return ChainService.MetaZkp.parseFrom(this.transaction)
}