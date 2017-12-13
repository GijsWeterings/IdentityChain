package nl.tudelft.cs4160.identitychain.connection

import android.util.Log
import com.google.protobuf.ByteString
import nl.tudelft.cs4160.identitychain.Peer
import nl.tudelft.cs4160.identitychain.Peer.bytesToHex
import nl.tudelft.cs4160.identitychain.block.TrustChainBlock
import nl.tudelft.cs4160.identitychain.block.TrustChainBlock.*
import nl.tudelft.cs4160.identitychain.block.ValidationResult
import nl.tudelft.cs4160.identitychain.block.ValidationResult.ValidationStatus.*
import nl.tudelft.cs4160.identitychain.database.TrustChainDBHelper
import nl.tudelft.cs4160.identitychain.message.MessageProto
import nl.tudelft.cs4160.identitychain.message.MessageProto.Message.newBuilder
import java.io.UnsupportedEncodingException
import java.security.KeyPair
import java.util.*


/**
 * Leaving this here so we can see what they were doing.
 */
class Communication(private val dbHelper: TrustChainDBHelper, private val keyPair: KeyPair, val listener: CommunicationListener) {

    protected val peers: MutableMap<String, ByteArray> = HashMap()

    val myPublicKey: ByteArray
        get() = keyPair.public.encoded
//    private var server: Server? = null

    /**
     * Send a crawl request to the peer.
     * @param peer The peer.
     * @param publicKey Public key of me.
     * @param seqNum Requested sequence number.
     */
    fun sendCrawlRequest(peer: Peer, publicKey: ByteArray, seqNum: Int) {
        var sq = seqNum
        if (seqNum == 0) {
            sq = dbHelper.getBlock(publicKey,
                    dbHelper.getMaxSeqNum(publicKey))?.sequenceNumber ?: GENESIS_SEQ
        }

        if (sq >= 0) {
            sq = Math.max(GENESIS_SEQ, sq)
        }

        Log.i(TAG, "Requesting crawl of node " + bytesToHex(publicKey) + ":" + sq)

        val crawlRequest = MessageProto.CrawlRequest.newBuilder()
                .setPublicKey(ByteString.copyFrom(myPublicKey))
                .setRequestedSequenceNumber(sq)
                .setLimit(100).build()

        // send the crawl request
        val message = newBuilder().setCrawlRequest(crawlRequest).build()
        sendMessage(peer, message)
    }


    /**
     * Sends a block to the connected peer.
     *
     * @param block - The block to be send
     */
    fun sendHalfBlock(peer: Peer, block: MessageProto.TrustChainBlock) {
        val message = newBuilder().setHalfBlock(block).build()
        sendMessage(peer, message)
    }


    /**
     * Sign a half block and send block.
     * Reads from database and inserts a new block in the database.
     *
     *
     * Either a linked half block is given to the function or a transaction that needs to be send
     *
     *
     * Similar to signblock of https://github.com/qstokkink/py-ipv8/blob/master/ipv8/attestation/trustchain/community.pyhttps://github.com/qstokkink/py-ipv8/blob/master/ipv8/attestation/trustchain/community.py
     */
    fun signBlock(peer: Peer, linkedBlock: MessageProto.TrustChainBlock) {
        // do nothing if linked block is not addressed to me
        if (!Arrays.equals(linkedBlock.linkPublicKey.toByteArray(), myPublicKey)) {
            Log.e(TAG, "signBlock: Linked block not addressed to me.")
            return
        }
        // do nothing if block is not a request
        if (linkedBlock.linkSequenceNumber != TrustChainBlock.UNKNOWN_SEQ) {
            Log.e(TAG, "signBlock: Block is not a request.")
            return
        }
        var block = createBlock(null, dbHelper,
                myPublicKey,
                linkedBlock, peer.publicKey)

        block = sign(block, keyPair.private)

        val validation: ValidationResult?
        try {
            validation = validate(block, dbHelper)
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        Log.i(TAG, "Signed block to " + bytesToHex(block.linkPublicKey.toByteArray()) +
                ", validation result: " + validation!!.toString())

        // only send block if validated correctly
        // If you want to test the sending of blocks and don't care whether or not blocks are valid, remove the next check.
        if (validation != null && validation.getStatus() !== PARTIAL_NEXT && validation.getStatus() !== VALID) {
            Log.e(TAG, "Signed block did not validate. Result: " + validation.toString() + ". Errors: "
                    + validation.getErrors().toString())
        } else {
            dbHelper.insertInDB(block)
            sendHalfBlock(peer, block)
        }
    }

    /**
     * Builds a half block with the transaction.
     * Reads from database and inserts new halfblock in database.
     *
     * @param transaction - a transaction which should be embedded in the block
     */
    fun signBlock(transaction: ByteArray?, peer: Peer) {
        if (transaction == null) {
            Log.e(TAG, "signBlock: Null transaction given.")
        }
        var block = createBlock(transaction, dbHelper,
                myPublicKey, null, peer.publicKey)
        block = sign(block, keyPair.private)

        val validation: ValidationResult?
        try {
            validation = validate(block, dbHelper)
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        Log.i(TAG, "Signed block to " + bytesToHex(block.linkPublicKey.toByteArray()) +
                ", validation result: " + validation!!.toString())

        // only send block if validated correctly
        // If you want to test the sending of blocks and don't care whether or not blocks are valid, remove the next check.
        if (validation != null && validation.getStatus() !== PARTIAL_NEXT && validation.getStatus() !== VALID) {
            Log.e(TAG, "Signed block did not validate. Result: " + validation.toString() + ". Errors: "
                    + validation.getErrors().toString())
        } else {
            dbHelper.insertInDB(block)
            sendHalfBlock(peer, block)
        }
    }

    /**
     * We have received a crawl request, this function handles what to do next.
     *
     * @param peer         - peer
     * @param crawlRequest - received crawl request
     */
    fun receivedCrawlRequest(peer: Peer, crawlRequest: MessageProto.CrawlRequest) {
        var sq = crawlRequest.requestedSequenceNumber

        Log.i(TAG, "Received crawl request from peer with IP: " + peer.ipAddress + ":" + peer.port +
                " and public key: \n" + bytesToHex(peer.publicKey) + "\n for sequence number " + sq)

        // a negative sequence number indicates that the requesting peer wants an offset of blocks
        // starting with the last block
        if (sq < 0) {
            val lastBlock = dbHelper.getLatestBlock(myPublicKey)

            if (lastBlock != null) {
                sq = Math.max(GENESIS_SEQ, lastBlock.sequenceNumber + sq + 1)
            } else {
                sq = GENESIS_SEQ
            }
        }

        val blockList = dbHelper.crawl(myPublicKey, sq)

        for (block in blockList) {
            sendHalfBlock(peer, block)
        }

        Log.i(TAG, "Sent " + blockList.size + " blocks")
    }

    /**
     * Act like we received a crawl request to send information about us to the peer.
     */
    fun sendLatestBlocksToPeer(peer: Peer) {
        val crawlRequest = MessageProto.CrawlRequest.newBuilder()
                .setPublicKey(ByteString.copyFrom(peer.publicKey))
                .setRequestedSequenceNumber(-5)
                .setLimit(100).build()
        receivedCrawlRequest(peer, crawlRequest)
    }


    /**
     * Process a received message. Checks if the message is a crawl request or a half block
     * and continues with the appropriate action,
     * @param message The message.
     * @param peer From the peer.
     */
    fun receivedMessage(message: MessageProto.Message, peer: Peer) {
        val block = message.halfBlock
        val crawlRequest = message.crawlRequest

        var messageLog = ""
        // In case we received a halfblock
        if (block.publicKey.size() > 0 && crawlRequest.publicKey.size() == 0) {
            messageLog += ("block received from: " + peer.ipAddress + ":"
                    + peer.port + "\n"
                    + TrustChainBlock.toShortString(block))

            listener.updateLog("\n  Server: " + messageLog)
            peer.publicKey = block.publicKey.toByteArray()

            //make sure the correct port is set
            peer.port = Communication.DEFAULT_PORT
            this.synchronizedReceivedHalfBlock(peer, block)
        }

        // In case we received a crawlrequest
        if (block.publicKey.size() == 0 && crawlRequest.publicKey.size() > 0) {
            messageLog += ("crawlrequest received from: " + peer.ipAddress + ":"
                    + peer.port)
            listener.updateLog("\n  Server: " + messageLog)

            peer.publicKey = crawlRequest.publicKey.toByteArray()
            this.receivedCrawlRequest(peer, crawlRequest)
        }
    }

    /**
     * A half block was send to us and received by us. Someone wants this peer to create the other half
     * and send it back. This method handles that 'request'.
     * - Checks if the block is valid and puts it in the database if not invalid.
     * - Checks if the block is addressed to me.
     * - Determines if we should sign the block
     * - Check if block matches with its previous block, send crawl request if more information is needed
     */
    fun synchronizedReceivedHalfBlock(peer: Peer, block: MessageProto.TrustChainBlock) {
        Log.i(TAG, "Received half block from peer with IP: " + peer.ipAddress + ":" + peer.port +
                " and public key: " + bytesToHex(peer.publicKey))

        addNewPublicKey(peer)

        val validation: ValidationResult
        try {
            validation = validate(block, dbHelper)
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        Log.i(TAG, "Received block validation result " + validation.toString() + "("
                + TrustChainBlock.toString(block) + ")")

        if (validation.getStatus() === INVALID) {
            for (error in validation.getErrors()) {
                Log.e(TAG, "Validation error: " + error)
            }
            return
        } else {
            dbHelper.insertInDB(block)
        }

        val pk = myPublicKey
        // check if addressed to me and if we did not sign it already, if so: do nothing.
        if (block.linkSequenceNumber != UNKNOWN_SEQ ||
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
        if (validation.getStatus() === PARTIAL_PREVIOUS || validation.getStatus() === PARTIAL ||
                validation.getStatus() === NO_INFO) {
            Log.e(TAG, "Request block could not be validated sufficiently, requested crawler. " + validation.toString())
            // send a crawl request, requesting the last 5 blocks before the received halfblock (if available) of the peer
            sendCrawlRequest(peer, block.publicKey.toByteArray(), Math.max(GENESIS_SEQ, block.sequenceNumber - 5))
        } else {
            signBlock(peer, block)
        }
    }


    /**
     * Connect with a peer, either unknown or known.
     * If the peer is not known, this will send a crawl request, otherwise a half block.
     * @param peer - The peer that we want to connect to
     */
    fun connectToPeer(peer: Peer, payload: String) {
        val identifier = peer.ipAddress
        Log.e(TAG, "Identifier: " + identifier)
        if (hasPublicKey(identifier)) {
            listener.updateLog("Sending half block to known peer")
            peer.publicKey = getPublicKey(identifier)
            sendLatestBlocksToPeer(peer)
            try {
                signBlock(payload.toByteArray(charset("UTF-8")), peer)
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }

        } else {
            listener.updateLog("Unknown peer, sending crawl request, when received press connect again")
            sendCrawlRequest(peer, myPublicKey, -5)
        }
    }


    fun hasPublicKey(identifier: String): Boolean {
        return peers.containsKey(identifier)
    }

    fun getPublicKey(identifier: String): ByteArray {
        return peers[identifier] ?: ByteArray(0)
    }

    fun addNewPublicKey(p: Peer) {
        peers[p.ipAddress] = p.publicKey
    }

    fun sendMessage(peer: Peer, message: MessageProto.Message) {
//        val task = ClientTask(
//                peer.ipAddress,
//                peer.port,
//                message,
//                listener)
//        task.execute()
    }

    fun start() {
//        val server1 = Server(this, listener)
//        server1.start()
//        server = server1
    }

    fun stop() {
        //TODO: make it stop listening
    }


    companion object {

        private val TAG = Communication::class.java.name
        val DEFAULT_PORT = 8080


        /**
         * Checks if we should sign the block. For now there is no reason to not sign a block.
         *
         * @param block - The block for which we might want to sign.
         * @return true
         */
        fun shouldSign(block: MessageProto.TrustChainBlock): Boolean {
            return true
        }
    }
}
