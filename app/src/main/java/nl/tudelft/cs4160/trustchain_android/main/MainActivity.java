package nl.tudelft.cs4160.trustchain_android.main;

import android.app.ActivityManager;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.protobuf.ByteString;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.tudelft.cs4160.trustchain_android.KeyActivity;
import nl.tudelft.cs4160.trustchain_android.Peer;
import nl.tudelft.cs4160.trustchain_android.R;
import nl.tudelft.cs4160.trustchain_android.Util.Key;
import nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock;
import nl.tudelft.cs4160.trustchain_android.block.ValidationResult;
import nl.tudelft.cs4160.trustchain_android.chainExplorer.ChainExplorerActivity;
import nl.tudelft.cs4160.trustchain_android.database.TrustChainDBContract;
import nl.tudelft.cs4160.trustchain_android.database.TrustChainDBHelper;
import nl.tudelft.cs4160.trustchain_android.message.MessageProto;

import static nl.tudelft.cs4160.trustchain_android.Peer.bytesToHex;
import static nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock.EMPTY_PK;
import static nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock.GENESIS_SEQ;
import static nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock.createBlock;
import static nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock.getLatestBlock;
import static nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock.sign;
import static nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock.validate;
import static nl.tudelft.cs4160.trustchain_android.block.ValidationResult.PARTIAL_NEXT;
import static nl.tudelft.cs4160.trustchain_android.block.ValidationResult.VALID;
import static nl.tudelft.cs4160.trustchain_android.database.TrustChainDBHelper.insertInDB;
import static nl.tudelft.cs4160.trustchain_android.message.MessageProto.Message.newBuilder;

public class MainActivity extends AppCompatActivity {
    final static String TRANSACTION = "Hello world!";
    private final static String TAG = MainActivity.class.toString();
    final static int DEFAULT_PORT = 8080;

    TrustChainDBHelper dbHelper;
    SQLiteDatabase db;
    SQLiteDatabase dbReadable;

    public Map<String,byte[]> peers;

    TextView externalIPText;
    TextView localIPText;
    TextView statusText;
    Button connectionButton;
    Button chainExplorerButton;
    Button resetDatabaseButton;
    Button keyOptionsButton;
    EditText editTextDestinationIP;
    EditText editTextDestinationPort;

    MainActivity thisActivity;

    /**
     * Key pair of user
     */
    KeyPair kp;

    /**
     * Listener for the connection button.
     * On click a block is created and send to a peer.
     * When we encounter an unknown peer, send a crawl request to that peer in order to get its
     * public key.
     * Also, when we want to send a block always send our last 5 blocks to the peer so the block
     * request won't be rejected due to NO_INFO error.
     *
     * This is code to simulate dispersy, note that this does not work properly with a busy network,
     * because the time delay between sending information to the peer and sending the actual
     * to-be-signed block could cause gaps.
     *
     * Also note that whatever goes wrong we will never get a valid full block, so the integrity of
     * the network is not compromised due to not using dispersy.
     */
    View.OnClickListener connectionButtonListener = new View.OnClickListener(){
        @Override
        public void onClick(View view) {
            String ipAddress = editTextDestinationIP.getText().toString();
            if (peers.containsKey(ipAddress)) {
                Peer peer = new Peer(
                        peers.get(ipAddress),
                        editTextDestinationIP.getText().toString(),
                        Integer.parseInt(editTextDestinationPort.getText().toString()));
                sendLatestBlocksToPeer(peer);
                try {
                    signBlock(TRANSACTION.getBytes("UTF-8"), peer);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
            else {
                Toast.makeText(getApplicationContext(),"Unknown peer, sending crawl request, when received press connect again",Toast.LENGTH_LONG).show();
                Peer peer = new Peer(
                        EMPTY_PK.toByteArray(),
                        editTextDestinationIP.getText().toString(),
                        Integer.parseInt(editTextDestinationPort.getText().toString()));
                sendCrawlRequest(peer,getMyPublicKey(),-5);
            }
        }
    };

    View.OnClickListener chainExplorerButtonListener = new View.OnClickListener(){
        @Override
        public void onClick(View view) {
            Intent intent = new Intent(thisActivity, ChainExplorerActivity.class);
            startActivity(intent);
        }
    };

    View.OnClickListener keyOptionsListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent intent = new Intent(thisActivity, KeyActivity.class);
            startActivity(intent);
        }
    };

    View.OnClickListener resetDatabaseListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (Build.VERSION_CODES.KITKAT <= Build.VERSION.SDK_INT) {
                ((ActivityManager) getApplicationContext().getSystemService(ACTIVITY_SERVICE))
                        .clearApplicationUserData();
            } else {
                Toast.makeText(getApplicationContext(), "Requires at least API 19 (KitKat)", Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initVariables();
        init();
    }

    private void initVariables() {
        thisActivity = this;
        localIPText = (TextView) findViewById(R.id.my_local_ip);
        externalIPText = (TextView) findViewById(R.id.my_external_ip);
        statusText = (TextView) findViewById(R.id.status);
        statusText.setMovementMethod(new ScrollingMovementMethod());
        editTextDestinationIP = (EditText) findViewById(R.id.destination_IP);
        editTextDestinationPort = (EditText) findViewById(R.id.destination_port);
        connectionButton = (Button) findViewById(R.id.connection_button);
        chainExplorerButton = (Button) findViewById(R.id.chain_explorer_button);
        resetDatabaseButton = (Button) findViewById(R.id.reset_database_button);
        keyOptionsButton = (Button) findViewById(R.id.key_options_button);
    }

    private void init() {
        dbHelper = new TrustChainDBHelper(thisActivity);
        db = dbHelper.getWritableDatabase();
        dbReadable = dbHelper.getReadableDatabase();

        peers = new HashMap<>();

        //create or load keys
        initKeys();

        if(isStartedFirstTime()) {
            MessageProto.TrustChainBlock block = TrustChainBlock.createGenesisBlock(kp);
            insertInDB(block, db);
        }

        updateIP();
        updateLocalIPField(getLocalIPAddress());

        connectionButton.setOnClickListener(connectionButtonListener);
        chainExplorerButton.setOnClickListener(chainExplorerButtonListener);
        keyOptionsButton.setOnClickListener(keyOptionsListener);
        resetDatabaseButton.setOnClickListener(resetDatabaseListener);
        Server socketServer = new Server(thisActivity);
        socketServer.start();
    }

    private void initKeys() {
        kp = Key.loadKeys(getApplicationContext());
        if(kp == null) {
            kp = Key.createNewKeyPair();
            Key.saveKey(getApplicationContext(), Key.DEFAULT_PUB_KEY_FILE, kp.getPublic());
            Key.saveKey(getApplicationContext(), Key.DEFAULT_PRIV_KEY_FILE, kp.getPrivate());
            Log.i(TAG, "New keys created" );
        }
    }

    /**
     * Checks if this is the first time the app is started and returns a boolean value indicating
     * this state.
     * @return state - false if the app has been initialized before, true if first time app started
     */
    public boolean isStartedFirstTime() {
        // check if a genesis block is present in database
        String[] projection = {
                TrustChainDBContract.BlockEntry.COLUMN_NAME_SEQUENCE_NUMBER,
        };

        String whereClause = TrustChainDBContract.BlockEntry.COLUMN_NAME_SEQUENCE_NUMBER + " = ?";
        String[] whereArgs = new String[] {Integer.toString(TrustChainBlock.GENESIS_SEQ)};

        Cursor cursor = dbReadable.query(
                TrustChainDBContract.BlockEntry.TABLE_NAME,     // Table name for the query
                projection,                                     // The columns to return
                whereClause,                                           // Filter for which rows to return
                whereArgs,                                           // Filter arguments
                null,                                           // Declares how to group rows
                null,                                           // Declares which row groups to include
                null                                           // How the rows should be ordered
        );
        if(cursor.getCount() == 1) {
            return false;
        }
        return true;
    }

    /**
     * Updates the external IP address textfield to the given IP address.
     */
    public void updateExternalIPField(String ipAddress) {
        externalIPText.setText(ipAddress);
        Log.i(TAG, "Updated external IP Address: " + ipAddress);
    }

    /**
     * Updates the internal IP address textfield to the given IP address.
     */
    public void updateLocalIPField(String ipAddress) {
        localIPText.setText(ipAddress);
        Log.i(TAG, "Updated local IP Address:" + ipAddress);
    }

    /**
     * Finds the external IP address of this device by making an API call to https://www.ipify.org/.
     * The networking runs on a separate thread.
     * @return a string representation of the device's external IP address
     */
    public void updateIP() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try (java.util.Scanner s = new java.util.Scanner(new java.net.URL("https://api.ipify.org").openStream(), "UTF-8").useDelimiter("\\A")) {
                    final String ip = s.next();
                    // new thread to handle UI updates
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateExternalIPField(ip);
                        }
                    });
                } catch (java.io.IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    /**
     * Finds the local IP address of this device, loops trough network interfaces in order to find it.
     * The address that is not a loopback address is the IP of the device.
     * @return a string representation of the device's IP address
     */
    public String getLocalIPAddress() {
        try {
            List<NetworkInterface> netInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface netInt : netInterfaces) {
                List<InetAddress> addresses = Collections.list(netInt.getInetAddresses());
                for (InetAddress addr : addresses) {
                    if(addr.isSiteLocalAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Sends a block to the connected peer.
     * @param block - The block to be send
     */
    public void sendBlock(Peer peer, MessageProto.TrustChainBlock block) {
        MessageProto.Message message = newBuilder().setHalfBlock(block).build();
        Log.i("SENDING", "Send: \n" + bytesToHex(message.getHalfBlock().getPublicKey().toByteArray()) );
        ClientTask task = new ClientTask(
                peer.getIpAddress(),
                peer.getPort(),
                message,
                thisActivity);
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

        block = sign(block, kp.getPrivate());

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
            sendBlock(peer,block);
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
        block = sign(block, kp.getPrivate());

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
            sendBlock(peer,block);
        }
    }

    public byte[] getMyPublicKey() {
        return kp.getPublic().getEncoded();
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
        ClientTask task = new ClientTask(
                peer.getIpAddress(),
                peer.getPort(),
                message,
                thisActivity);
        task.execute();
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
            sendBlock(peer,block);
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
}
