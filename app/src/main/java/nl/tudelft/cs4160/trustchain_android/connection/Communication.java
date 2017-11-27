package nl.tudelft.cs4160.trustchain_android.connection;

import android.util.Log;

import com.google.protobuf.ByteString;

import java.io.UnsupportedEncodingException;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.tudelft.cs4160.trustchain_android.Peer;
import nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock;
import nl.tudelft.cs4160.trustchain_android.block.ValidationResult;
import nl.tudelft.cs4160.trustchain_android.connection.network.NetworkCommunication;
import nl.tudelft.cs4160.trustchain_android.database.TrustChainDBHelper;
import nl.tudelft.cs4160.trustchain_android.main.MainActivity;
import nl.tudelft.cs4160.trustchain_android.message.MessageProto;

import static nl.tudelft.cs4160.trustchain_android.Peer.bytesToHex;
import static nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock.GENESIS_SEQ;
import static nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock.UNKNOWN_SEQ;
import static nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock.createBlock;
import static nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock.sign;
import static nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock.validate;
import static nl.tudelft.cs4160.trustchain_android.block.ValidationResult.NO_INFO;
import static nl.tudelft.cs4160.trustchain_android.block.ValidationResult.PARTIAL;
import static nl.tudelft.cs4160.trustchain_android.block.ValidationResult.PARTIAL_NEXT;
import static nl.tudelft.cs4160.trustchain_android.block.ValidationResult.PARTIAL_PREVIOUS;
import static nl.tudelft.cs4160.trustchain_android.block.ValidationResult.VALID;
import static nl.tudelft.cs4160.trustchain_android.message.MessageProto.Message.newBuilder;


/**
 * Class that is responsible for the communication.
 */
public abstract class Communication {

    private static final String TAG = Communication.class.getName();

    private Map<String, byte[]> peers;


    private TrustChainDBHelper dbHelper;

    private KeyPair keyPair;

    private CommunicationListener listener;


    public Communication(TrustChainDBHelper dbHelper, KeyPair kp, CommunicationListener listener) {
        this.dbHelper = dbHelper;
        this.keyPair = kp;
        this.listener = listener;
        this.peers = new HashMap<>();

    }

    public CommunicationListener getListener() {
        return listener;
    }

    /**
     * Send a crawl request to the peer.
     * @param peer The peer.
     * @param publicKey Public key of me.
     * @param seqNum Requested sequence number.
     */
    public void sendCrawlRequest(Peer peer, byte[] publicKey, int seqNum) {
        int sq = seqNum;
        if (seqNum == 0) {
            MessageProto.TrustChainBlock block = dbHelper.getBlock(publicKey,
                    dbHelper.getMaxSeqNum(publicKey));
            if (block != null) {
                sq = block.getSequenceNumber();
            } else {
                sq = GENESIS_SEQ;
            }
        }

        if (sq >= 0) {
            sq = Math.max(GENESIS_SEQ, sq);
        }

        Log.i(TAG, "Requesting crawl of node " + bytesToHex(publicKey) + ":" + sq);

        MessageProto.CrawlRequest crawlRequest =
                MessageProto.CrawlRequest.newBuilder()
                        .setPublicKey(ByteString.copyFrom(getMyPublicKey()))
                        .setRequestedSequenceNumber(sq)
                        .setLimit(100).build();

        // send the crawl request
        MessageProto.Message message = newBuilder().setCrawlRequest(crawlRequest).build();
        sendMessage(peer, message);
    }


    /**
     * Sends a block to the connected peer.
     *
     * @param block - The block to be send
     */
    public void sendHalfBlock(Peer peer, MessageProto.TrustChainBlock block) {
        MessageProto.Message message = newBuilder().setHalfBlock(block).build();
        sendMessage(peer, message);
    }

    /**
     * Send either a crawl request of a block to a peer.
     *
     * @param peer    The peer
     * @param message The message.
     */
    public abstract void sendMessage(Peer peer, MessageProto.Message message);

    /**
     * Start listening for messages.
     */
    public abstract void start();


    /**
     * Stop listening to messages
     */
    public abstract void stop();

    /**
     * Sign a half block and send block.
     * Reads from database and inserts a new block in the database.
     * <p>
     * Either a linked half block is given to the function or a transaction that needs to be send
     * <p>
     * Similar to signblock of https://github.com/qstokkink/py-ipv8/blob/master/ipv8/attestation/trustchain/community.pyhttps://github.com/qstokkink/py-ipv8/blob/master/ipv8/attestation/trustchain/community.py
     */
    public void signBlock(Peer peer, MessageProto.TrustChainBlock linkedBlock) {
        // assert that the linked block is not null
        if (linkedBlock == null) {
            Log.e(TAG, "signBlock: Linked block is null.");
            return;
        }
        // do nothing if linked block is not addressed to me
        if (!Arrays.equals(linkedBlock.getLinkPublicKey().toByteArray(), getMyPublicKey())) {
            Log.e(TAG, "signBlock: Linked block not addressed to me.");
            return;
        }
        // do nothing if block is not a request
        if (linkedBlock.getLinkSequenceNumber() != TrustChainBlock.UNKNOWN_SEQ) {
            Log.e(TAG, "signBlock: Block is not a request.");
            return;
        }
        MessageProto.TrustChainBlock block = createBlock(null,dbHelper,
                getMyPublicKey(),
                linkedBlock,peer.getPublicKey());

        block = sign(block, keyPair.getPrivate());

        ValidationResult validation;
        try {
            validation = validate(block, dbHelper);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        Log.i(TAG, "Signed block to " + bytesToHex(block.getLinkPublicKey().toByteArray()) +
                ", validation result: " + validation.toString());

        // only send block if validated correctly
        // If you want to test the sending of blocks and don't care whether or not blocks are valid, remove the next check.
        if (validation != null && validation.getStatus() != PARTIAL_NEXT && validation.getStatus() != VALID) {
            Log.e(TAG, "Signed block did not validate. Result: " + validation.toString() + ". Errors: "
                    + validation.getErrors().toString());
        } else {
            dbHelper.insertInDB(block);
            sendHalfBlock(peer, block);
        }
    }

    /**
     * Builds a half block with the transaction.
     * Reads from database and inserts new halfblock in database.
     *
     * @param transaction - a transaction which should be embedded in the block
     */
    public void signBlock(byte[] transaction, Peer peer) {
        if (transaction == null) {
            Log.e(TAG, "signBlock: Null transaction given.");
        }
        MessageProto.TrustChainBlock block =
                createBlock(transaction,dbHelper,
                        getMyPublicKey(),null,peer.getPublicKey());
        block = sign(block, keyPair.getPrivate());

        ValidationResult validation;
        try {
            validation = validate(block, dbHelper);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        Log.i(TAG, "Signed block to " + bytesToHex(block.getLinkPublicKey().toByteArray()) +
                ", validation result: " + validation.toString());

        // only send block if validated correctly
        // If you want to test the sending of blocks and don't care whether or not blocks are valid, remove the next check.
        if (validation != null && validation.getStatus() != PARTIAL_NEXT && validation.getStatus() != VALID) {
            Log.e(TAG, "Signed block did not validate. Result: " + validation.toString() + ". Errors: "
                    + validation.getErrors().toString());
        } else {
            dbHelper.insertInDB(block);
            sendHalfBlock(peer, block);
        }
    }



    /**
     * Checks if we should sign the block. For now there is no reason to not sign a block.
     *
     * @param block - The block for which we might want to sign.
     * @return true
     */
    public static boolean shouldSign(MessageProto.TrustChainBlock block) {
        return true;
    }

    /**
     * We have received a crawl request, this function handles what to do next.
     *
     * @param peer         - peer
     * @param crawlRequest - received crawl request
     */
    public void receivedCrawlRequest(Peer peer, MessageProto.CrawlRequest crawlRequest) {
        int sq = crawlRequest.getRequestedSequenceNumber();

        Log.i(TAG, "Received crawl request from peer with IP: " + peer.getIpAddress() + ":" + peer.getPort() +
                " and public key: \n" + bytesToHex(peer.getPublicKey()) + "\n for sequence number " + sq);

        // a negative sequence number indicates that the requesting peer wants an offset of blocks
        // starting with the last block
        if (sq < 0) {
            MessageProto.TrustChainBlock lastBlock = dbHelper.getLatestBlock(getMyPublicKey());

            if (lastBlock != null) {
                sq = Math.max(GENESIS_SEQ, lastBlock.getSequenceNumber() + sq + 1);
            } else {
                sq = GENESIS_SEQ;
            }
        }

        List<MessageProto.TrustChainBlock> blockList = dbHelper.crawl(getMyPublicKey(), sq);

        for (MessageProto.TrustChainBlock block : blockList) {
            sendHalfBlock(peer, block);
        }

        Log.i(TAG, "Sent " + blockList.size() + " blocks");
    }

    /**
     * Act like we received a crawl request to send information about us to the peer.
     */
    public void sendLatestBlocksToPeer(Peer peer) {
        MessageProto.CrawlRequest crawlRequest =
                MessageProto.CrawlRequest.newBuilder()
                        .setPublicKey(ByteString.copyFrom(peer.getPublicKey()))
                        .setRequestedSequenceNumber(-5)
                        .setLimit(100).build();
        receivedCrawlRequest(peer, crawlRequest);
    }


    /**
     * Process a received message. Checks if the message is a crawl request or a half block
     * and continues with the appropriate action,
     * @param message The message.
     * @param peer From the peer.
     */
    public void receivedMessage(MessageProto.Message message, Peer peer) {
        MessageProto.TrustChainBlock block = message.getHalfBlock();
        MessageProto.CrawlRequest crawlRequest = message.getCrawlRequest();

        String messageLog = "";
        // In case we received a halfblock
        if (block.getPublicKey().size() > 0 && crawlRequest.getPublicKey().size() == 0) {
            messageLog += "block received from: " + peer.getIpAddress() + ":"
                    + peer.getPort() + "\n"
                    + TrustChainBlock.toShortString(block);

            listener.updateLog("\n  Server: " + messageLog);
            peer.setPublicKey(block.getPublicKey().toByteArray());

            //make sure the correct port is set
            peer.setPort(NetworkCommunication.DEFAULT_PORT);
            this.synchronizedReceivedHalfBlock(peer, block);
        }

        // In case we received a crawlrequest
        if (block.getPublicKey().size() == 0 && crawlRequest.getPublicKey().size() > 0) {
            messageLog += "crawlrequest received from: " + peer.getIpAddress() + ":"
                    + peer.getPort();
            listener.updateLog("\n  Server: " + messageLog);

            peer.setPublicKey(crawlRequest.getPublicKey().toByteArray());
            this.receivedCrawlRequest(peer, crawlRequest);
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
    public void synchronizedReceivedHalfBlock(Peer peer, MessageProto.TrustChainBlock block) {
        Log.i(TAG, "Received half block from peer with IP: " + peer.getIpAddress() + ":" + peer.getPort() +
                " and public key: " + bytesToHex(peer.getPublicKey()));

        addNewPublicKey(peer);

        ValidationResult validation;
        try {
            validation = validate(block, dbHelper);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        Log.i(TAG, "Received block validation result " + validation.toString() + "("
                + TrustChainBlock.toString(block) + ")");

        if (validation.getStatus() == ValidationResult.INVALID) {
            for (String error : validation.getErrors()) {
                Log.e(TAG, "Validation error: " + error);
            }
            return;
        } else {
            dbHelper.insertInDB(block);
        }

        byte[] pk = getMyPublicKey();
        // check if addressed to me and if we did not sign it already, if so: do nothing.
        if (block.getLinkSequenceNumber() != UNKNOWN_SEQ ||
                !Arrays.equals(block.getLinkPublicKey().toByteArray(), pk) ||
                null != dbHelper.getBlock(block.getLinkPublicKey().toByteArray(),
                        block.getLinkSequenceNumber())) {
            Log.e(TAG, "Received block not addressed to me or already signed by me.");
            return;
        }

        // determine if we should sign the block, if not: do nothing
        if (!Communication.shouldSign(block)) {
            Log.e(TAG, "Will not sign received block.");
            return;
        }

        // check if block matches up with its previous block
        // At this point gaps cannot be tolerated. If we detect a gap we send crawl requests to fill
        // the gap and delay the method until the gap is filled.
        // Note that this code does not cover the scenario where we obtain this block indirectly,
        // because the code does nothing with this block after the crawlRequest was received.
        if (validation.getStatus() == PARTIAL_PREVIOUS || validation.getStatus() == PARTIAL ||
                validation.getStatus() == NO_INFO) {
            Log.e(TAG, "Request block could not be validated sufficiently, requested crawler. " +
                    validation.toString());
            // send a crawl request, requesting the last 5 blocks before the received halfblock (if available) of the peer
            sendCrawlRequest(peer, block.getPublicKey().toByteArray(), Math.max(GENESIS_SEQ, block.getSequenceNumber() - 5));
        } else {
            signBlock(peer, block);
        }
    }


    /**
     * Connect with a peer, either unknown or known.
     * If the peer is not known, this will send a crawl request, otherwise a half block.
     * @param peer - The peer that we want to connect to
     */
    public void connectToPeer(Peer peer) {
        String identifier = peer.getIpAddress();
        if(peer.getDevice() != null) {
            identifier = peer.getDevice().getAddress();
        }
        Log.e(TAG, "Identifier: " + identifier);
        if (hasPublicKey(identifier)) {
            listener.updateLog("Sending half block to known peer");
            peer.setPublicKey(getPublicKey(identifier));
            sendLatestBlocksToPeer(peer);
            try {
                signBlock(MainActivity.TRANSACTION.getBytes("UTF-8"), peer);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else {
            listener.updateLog("Unknown peer, sending crawl request, when received press connect again");
            sendCrawlRequest(peer, getMyPublicKey(),-5);
        }
    }

    public byte[] getMyPublicKey() {
        return keyPair.getPublic().getEncoded();
    }


    protected Map<String, byte[]> getPeers() {
        return peers;
    }



    public boolean hasPublicKey(String identifier) {
        return peers.containsKey(identifier);
    }

    public byte[] getPublicKey(String identifier) {
        return getPeers().get(identifier);
    }

    public abstract void addNewPublicKey(Peer p);
}
