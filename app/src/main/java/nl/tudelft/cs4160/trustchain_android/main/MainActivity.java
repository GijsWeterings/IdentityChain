package nl.tudelft.cs4160.trustchain_android.main;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import nl.tudelft.cs4160.trustchain_android.ChainExplorerActivity;
import nl.tudelft.cs4160.trustchain_android.Peer;
import nl.tudelft.cs4160.trustchain_android.R;
import nl.tudelft.cs4160.trustchain_android.block.BlockProto;
import nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock;
import nl.tudelft.cs4160.trustchain_android.block.ValidationResult;
import nl.tudelft.cs4160.trustchain_android.database.TrustChainDBContract;
import nl.tudelft.cs4160.trustchain_android.database.TrustChainDBHelper;

import static nl.tudelft.cs4160.trustchain_android.Peer.bytesToHex;
import static nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock.EMPTY_PK;
import static nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock.TEMP_PEER_PK;
import static nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock.createBlock;
import static nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock.sign;
import static nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock.validate;
import static nl.tudelft.cs4160.trustchain_android.block.ValidationResult.PARTIAL_NEXT;
import static nl.tudelft.cs4160.trustchain_android.block.ValidationResult.VALID;
import static nl.tudelft.cs4160.trustchain_android.database.TrustChainDBHelper.insertInDB;

public class MainActivity extends AppCompatActivity {
    final static String TRANSACTION = "Hello world!";
    private static final String TAG = "MainActivity";

    BlockProto.TrustChainBlock message;
    TrustChainDBHelper dbHelper;
    SQLiteDatabase db;
    SQLiteDatabase dbReadable;

    TextView externalIPText;
    TextView localIPText;
    TextView statusText;
    Button connectionButton;
    Button chainExplorerButton;
    EditText editTextDestinationIP;
    EditText editTextDestinationPort;

    MainActivity thisActivity;

    /**
     * Listener for the connection button.
     * On click a block is created and send to a peer.
     * TODO: For now a halfblock is created and send, this should be changed to first sending a crawl
     * TODO: request to either get some information on the peer, like its pubKey which is needed for
     * TODO: building a block. Or to check whether the information we have associated with this IP
     * TODO: is still correct. (although we can never get a valid full block anyway when we send it
     * TODO: to the wrong person)
     */
    View.OnClickListener connectionButtonListener = new View.OnClickListener(){
        @Override
        public void onClick(View view) {
            Peer peer = new Peer(
                    TEMP_PEER_PK.toByteArray(),
                    editTextDestinationIP.getText().toString(),
                    Integer.parseInt(editTextDestinationPort.getText().toString()));
            try {
                signBlock(TRANSACTION.getBytes("UTF-8"),peer);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
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
    }

    private void init() {
        // TODO: key generation
        dbHelper = new TrustChainDBHelper(thisActivity);
        db = dbHelper.getWritableDatabase();
        dbReadable = dbHelper.getReadableDatabase();

        if(isStartedFirstTime()) {
            message = TrustChainBlock.createGenesisBlock();
            insertInDB(message, db);
        }

        updateIP();
        updateLocalIPField(getLocalIPAddress());

        connectionButton.setOnClickListener(connectionButtonListener);
        chainExplorerButton.setOnClickListener(chainExplorerButtonListener);
        Server socketServer = new Server(thisActivity);
        socketServer.start();
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

        // TODO: check if a keypair is already created
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
    public void sendBlock(Peer peer, BlockProto.TrustChainBlock block) {
        ClientTask task = new ClientTask(
                peer.getIpAddress(),
                peer.getPort(),
                block,
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
    public void signBlock(Peer peer, BlockProto.TrustChainBlock linkedBlock) {
        // do nothing if linked block is not addressed to me
        if(!linkedBlock.getLinkPublicKey().equals(getMyPublicKey())){
            return;
        }
        // do nothing if block is not a request
        if(linkedBlock.getLinkSequenceNumber() != TrustChainBlock.UNKNOWN_SEQ){
            return;
        }
        BlockProto.TrustChainBlock block = createBlock(null,dbReadable,
                getMyPublicKey(),
                linkedBlock,null);

        sign(block, getMyPublicKey());

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
        if(validation != null && validation.getStatus() != PARTIAL_NEXT && validation.getStatus() != VALID) {
            Log.e(TAG, "Signed block did not validate. Result: " + validation.toString());
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
        BlockProto.TrustChainBlock block =
                createBlock(transaction,dbReadable,
                        getMyPublicKey(),null,peer.getPublicKey());
        sign(block,getMyPublicKey());

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


    // Placeholder TODO: change all places where this method gets called to correct method
    public static byte[] getMyPublicKey() {
        return EMPTY_PK.toByteArray();
    }

}
