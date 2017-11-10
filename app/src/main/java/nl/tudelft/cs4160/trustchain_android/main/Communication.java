package nl.tudelft.cs4160.trustchain_android.main;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.protobuf.ByteString;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.tudelft.cs4160.trustchain_android.Peer;
import nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock;
import nl.tudelft.cs4160.trustchain_android.block.ValidationResult;
import nl.tudelft.cs4160.trustchain_android.database.TrustChainDBHelper;
import nl.tudelft.cs4160.trustchain_android.message.MessageProto;

import static nl.tudelft.cs4160.trustchain_android.Peer.bytesToHex;
import static nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock.GENESIS_SEQ;
import static nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock.UNKNOWN_SEQ;
import static nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock.createBlock;
import static nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock.getBlock;
import static nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock.getLatestBlock;
import static nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock.sign;
import static nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock.validate;
import static nl.tudelft.cs4160.trustchain_android.block.ValidationResult.NO_INFO;
import static nl.tudelft.cs4160.trustchain_android.block.ValidationResult.PARTIAL;
import static nl.tudelft.cs4160.trustchain_android.block.ValidationResult.PARTIAL_NEXT;
import static nl.tudelft.cs4160.trustchain_android.block.ValidationResult.PARTIAL_PREVIOUS;
import static nl.tudelft.cs4160.trustchain_android.block.ValidationResult.VALID;
import static nl.tudelft.cs4160.trustchain_android.database.TrustChainDBHelper.insertInDB;
import static nl.tudelft.cs4160.trustchain_android.main.MainActivity.DEFAULT_PORT;
import static nl.tudelft.cs4160.trustchain_android.message.MessageProto.Message.newBuilder;

/**
 * Created by rico on 10-11-17.
 */

public class Communication {

    private static final String TAG = Communication.class.getName();

    public Map<String,byte[]> peers;


    private TrustChainDBHelper dbHelper;
    private SQLiteDatabase db;
    private SQLiteDatabase dbReadable;

    private KeyPair keyPair;

    private CommunicationListener listener;


    public Communication(TrustChainDBHelper dbHelper, KeyPair kp, CommunicationListener listener) {
        this.dbHelper = dbHelper;
        this.dbReadable = this.dbHelper.getReadableDatabase();
        this.db = this.dbHelper.getWritableDatabase();
        this.keyPair = kp;
        this.listener = listener;
        this.peers = new HashMap<>();

    }

    public void sendCrawlRequest(Peer peer, byte[] publicKey, int seqNum) {
        int sq = seqNum;
        if(seqNum == 0) {
            MessageProto.TrustChainBlock block = TrustChainBlock.getBlock(dbReadable,publicKey,
                    TrustChainBlock.getMaxSeqNum(dbReadable,publicKey));
            if(block != null) {
                sq = block.getSequenceNumber();
            } else {
                sq = GENESIS_SEQ;
            }
        }

        // TODO: check this: This piece of code is in python, but I'm not sure what it adds.
        if(sq >= 0) {
            sq = Math.max(GENESIS_SEQ, sq);
        }

        Log.i(TAG,"Requesting crawl of node " + bytesToHex(publicKey) + ":" + sq);

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
     * @param block - The block to be send
     */
    public void sendHalfBlock(Peer peer, MessageProto.TrustChainBlock block) {
        MessageProto.Message message = newBuilder().setHalfBlock(block).build();
        sendMessage(peer, message);
    }

    /**
     * Send either a crawl request of a block to a peer.
     * @param peer The peer
     * @param message The message.
     */
    public void sendMessage(Peer peer, MessageProto.Message message) {
        ClientTask task = new ClientTask(
                peer.getIpAddress(),
                peer.getPort(),
                message,
                listener);
        task.execute();
    }


    /**
     * Sign a half block and send block.
     * Reads from database and inserts a new block in the database.
     *
     * Either a linked half block is given to the function or a transaction that needs to be send
     *
     * Similar to signblock of https://github.com/qstokkink/py-ipv8/blob/master/ipv8/attestation/trustchain/community.pyhttps://github.com/qstokkink/py-ipv8/blob/master/ipv8/attestation/trustchain/community.py
     */
    public void signBlock(Peer peer, MessageProto.TrustChainBlock linkedBlock) {
        // assert that the linked block is not null
        if(linkedBlock == null){
            Log.e(TAG,"signBlock: Linked block is null.");
            return;
        }
        // do nothing if linked block is not addressed to me
        if(!Arrays.equals(linkedBlock.getLinkPublicKey().toByteArray(),getMyPublicKey())){
            Log.e(TAG,"signBlock: Linked block not addressed to me.");
            return;
        }
        // do nothing if block is not a request
        if(linkedBlock.getLinkSequenceNumber() != TrustChainBlock.UNKNOWN_SEQ){
            Log.e(TAG,"signBlock: Block is not a request.");
            return;
        }
        MessageProto.TrustChainBlock block = createBlock(null,dbReadable,
                getMyPublicKey(),
                linkedBlock,peer.getPublicKey());

        block = sign(block, keyPair.getPrivate());

        ValidationResult validation;
        try {
            validation = validate(block,dbHelper);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        Log.i(TAG,"Signed block to " + bytesToHex(block.getLinkPublicKey().toByteArray()) +
                ", validation result: " + validation.toString());

        // only send block if validated correctly
        // If you want to test the sending of blocks and don't care whether or not blocks are valid, remove the next check.
        if(validation != null && validation.getStatus() != PARTIAL_NEXT && validation.getStatus() != VALID) {
            Log.e(TAG, "Signed block did not validate. Result: " + validation.toString() + ". Errors: "
                    + validation.getErrors().toString());
        } else {
            insertInDB(block,db);
            sendHalfBlock(peer,block);
        }
    }

    /**
     * Builds a half block with the transaction.
     * Reads from database and inserts new halfblock in database.
     * @param transaction - a transaction which should be embedded in the block
     */
    public void signBlock(byte[] transaction, Peer peer) {
        if(transaction == null) {
            Log.e(TAG,"signBlock: Null transaction given.");
        }
        MessageProto.TrustChainBlock block =
                createBlock(transaction,dbReadable,
                        getMyPublicKey(),null,peer.getPublicKey());
        block = sign(block, keyPair.getPrivate());

        ValidationResult validation;
        try {
            validation = validate(block,dbHelper);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        Log.i(TAG,"Signed block to " + bytesToHex(block.getLinkPublicKey().toByteArray()) +
                ", validation result: " + validation.toString());

        // only send block if validated correctly
        // If you want to test the sending of blocks and don't care whether or not blocks are valid, remove the next check.
        if(validation != null && validation.getStatus() != PARTIAL_NEXT && validation.getStatus() != VALID) {
            Log.e(TAG, "Signed block did not validate. Result: " + validation.toString() + ". Errors: "
                    + validation.getErrors().toString());
        } else {
            insertInDB(block,db);
            sendHalfBlock(peer,block);
        }
    }



    /**
     * Retrieves the dbhelper from mainactivity.
     * @return
     */
    public TrustChainDBHelper getDbHelper() {
        return dbHelper;
    }

    /**
     * Checks if we should sign the block. For now there is no reason to not sign a block.
     * @param block - The block for which we might want to sign.
     * @return
     */
    public static boolean shouldSign(MessageProto.TrustChainBlock block) {
        return true;
    }

    /**
     * We have received a crawl request, this function handles what to do next.
     *
     * @param address - ip address of the sending peer
     * @param port - port of the sending peer
     * @param crawlRequest - received crawl request
     */
    public void receivedCrawlRequest(InetAddress address, int port, MessageProto.CrawlRequest crawlRequest) {
        byte[] peerPubKey = crawlRequest.getPublicKey().toByteArray();
        Peer peer = new Peer(peerPubKey, address.getHostAddress(), port);
        int sq = crawlRequest.getRequestedSequenceNumber();

        Log.i(TAG, "Received crawl request from peer with IP: " + peer.getIpAddress() + ":" + peer.getPort() +
                " and public key: \n" + bytesToHex(peer.getPublicKey()) + "\n for sequence number " + sq);

        // a negative sequence number indicates that the requesting peer wants an offset of blocks
        // starting with the last block
        if(sq<0) {
            MessageProto.TrustChainBlock lastBlock = getLatestBlock(dbReadable, getMyPublicKey());

            if(lastBlock != null){
                sq = Math.max(GENESIS_SEQ, lastBlock.getSequenceNumber() + sq + 1);
            } else {
                sq = GENESIS_SEQ;
            }
        }

        List<MessageProto.TrustChainBlock> blockList = dbHelper.crawl(getMyPublicKey(),sq);

        for(MessageProto.TrustChainBlock block : blockList) {
            sendHalfBlock(peer,block);
        }

        Log.i(TAG,"Sent " + blockList.size() + " blocks");
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
        InetAddress address = null;
        try {
            address = InetAddress.getByName(peer.getIpAddress());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        receivedCrawlRequest(address,peer.getPort(),crawlRequest);
    }



    public void receiveMessage(MessageProto.Message message, InetAddress ip, int port) {
        MessageProto.TrustChainBlock block = message.getHalfBlock();
        MessageProto.CrawlRequest crawlRequest = message.getCrawlRequest();

        String messageLog = "";
        // In case we received a halfblock
        if(block.getPublicKey().size() > 0 && crawlRequest.getPublicKey().size() == 0) {
            messageLog += "block received from: " + ip + ":"
                    + port + "\n"
                    + TrustChainBlock.toShortString(block);

            listener.updateLog("\n  Server: " + messageLog);

            synchronizedReceivedHalfBlock(ip, port, block);
        }

        // In case we received a crawlrequest
        if(block.getPublicKey().size() == 0 && crawlRequest.getPublicKey().size() > 0) {
            messageLog += "crawlrequest received from: " + ip + ":"
                    + port;
            listener.updateLog("\n  Server: " + messageLog);

            this.receivedCrawlRequest(ip,
                    port, crawlRequest);
        }
    }

    /**
     * A half block was send to us and received by us. Someone wants this peer to create the other half
     * and send it back. This method handles that 'request'.
     *  - Checks if the block is valid and puts it in the database if not invalid.
     *  - Checks if the block is addressed to me.
     *  - Determines if we should sign the block
     *  - Check if block matches with its previous block, send crawl request if more information is needed
     */
    public void synchronizedReceivedHalfBlock(InetAddress address, int port, MessageProto.TrustChainBlock block) {
        Peer peer = new Peer(block.getPublicKey().toByteArray(), address.getHostAddress(), DEFAULT_PORT);
        Log.i(TAG, "Received half block from peer with IP: " + peer.getIpAddress() + ":" + peer.getPort() +
                " and public key: " + bytesToHex(peer.getPublicKey()));

        addNewPublicKey(peer);

        ValidationResult validation;
        try {
            validation = validate(block,dbHelper);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        Log.i(TAG,"Received block validation result " + validation.toString() + "("
                + TrustChainBlock.toString(block) + ")");

        if(validation.getStatus() == ValidationResult.INVALID) {
            for(String error: validation.getErrors()) {
                Log.e(TAG, "Validation error: " + error);
            }
            return;
        } else {
            insertInDB(block,dbHelper.getWritableDatabase());
        }

        byte[] pk = getMyPublicKey();
        // check if addressed to me and if we did not sign it already, if so: do nothing.
        if(block.getLinkSequenceNumber() != UNKNOWN_SEQ ||
                !Arrays.equals(block.getLinkPublicKey().toByteArray(), pk) ||
                null != getBlock(dbHelper.getReadableDatabase(),
                        block.getLinkPublicKey().toByteArray(),
                        block.getLinkSequenceNumber())) {
            Log.e(TAG,"Received block not addressed to me or already signed by me.");
            return;
        }

        // determine if we should sign the block, if not: do nothing
        if(!Communication.shouldSign(block)) {
            Log.e(TAG,"Will not sign received block.");
            return;
        }

        // check if block matches up with its previous block
        // At this point gaps cannot be tolerated. If we detect a gap we send crawl requests to fill
        // the gap and delay the method until the gap is filled.
        // Note that this code does not cover the scenario where we obtain this block indirectly,
        // because the code does nothing with this block after the crawlRequest was received.
        if(validation.getStatus() == PARTIAL_PREVIOUS || validation.getStatus() == PARTIAL ||
                validation.getStatus() == NO_INFO) {
            Log.e(TAG, "Request block could not be validated sufficiently, requested crawler. " +
                    validation.toString());
            // send a crawl request, requesting the last 5 blocks before the received halfblock (if available) of the peer
            sendCrawlRequest(peer,block.getPublicKey().toByteArray(),Math.max(GENESIS_SEQ,block.getSequenceNumber()-5));
        } else {
            signBlock(peer, block);
        }
    }

    public byte[] getMyPublicKey() {
        return keyPair.getPublic().getEncoded();
    }


    public boolean hasPublicKey(String ip) {
        return peers.containsKey(ip);
    }

    public byte[] getPublicKey(String ip) {
        return peers.get(ip);
    }

    public void addNewPublicKey(Peer p) {
        this.peers.put(p.getIpAddress(),p.getPublicKey());
    }
}
