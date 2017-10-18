package nl.tudelft.cs4160.trustchain_android.main;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.security.PublicKey;
import java.util.Collections;
import java.util.List;

import nl.tudelft.cs4160.trustchain_android.ChainExplorerActivity;
import nl.tudelft.cs4160.trustchain_android.R;
import nl.tudelft.cs4160.trustchain_android.block.BlockProto;
import nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock;
import nl.tudelft.cs4160.trustchain_android.database.TrustChainDBContract;
import nl.tudelft.cs4160.trustchain_android.database.TrustChainDBHelper;

import static nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock.EMPTY_PK;
import static nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock.createBlock;
import static nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock.createTestBlock;
import static nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock.getLatestBlock;
import static nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock.getMaxSeqNum;
import static nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock.sign;
import static nl.tudelft.cs4160.trustchain_android.database.TrustChainDBHelper.insertInDB;

public class MainActivity extends AppCompatActivity {
    final static String TRANSACTION = "Hello world!";

    BlockProto.TrustChainBlock message;
    TrustChainDBHelper dbHelper;
    SQLiteDatabase db;

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
     * On click a message is sent to the connected device.
     * This message should already be in the database.
     */
    View.OnClickListener connectionButtonListener = new View.OnClickListener(){
        @Override
        public void onClick(View view) {
            try {
                signBlock(TRANSACTION.getBytes("UTF-8"),EMPTY_PK.toByteArray());
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
        SQLiteDatabase dbReadable = dbHelper.getReadableDatabase();
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
        System.out.println("IP ADDRESS: " + ipAddress);
    }

    /**
     * Updates the internal IP address textfield to the given IP address.
     */
    public void updateLocalIPField(String ipAddress) {
        localIPText.setText(ipAddress);
        System.out.println("IP ADDRESS: " + ipAddress);
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

    //TODO: remove
    public void signBlock() {
        System.out.println("MAX SEQ NUM IN DB " + getMaxSeqNum(dbHelper.getReadableDatabase(),EMPTY_PK.toByteArray()));
        System.out.println("LATEST BLOCK IN DB:\n" + getLatestBlock(dbHelper.getReadableDatabase(),EMPTY_PK.toByteArray()));
        ClientTask task = new ClientTask(
                editTextDestinationIP.getText().toString(),
                Integer.parseInt(editTextDestinationPort.getText().toString()),
                createTestBlock(),
                thisActivity);
        task.execute();
        //TODO: for testing purposes, block insertion in DB must be done in another place
        insertInDB(createTestBlock(), db);
    }

    /**
     * Sends a block to the connected peer.
     * @param block - The block to be send
     */
    public void sendBlock(BlockProto.TrustChainBlock block) {
        ClientTask task = new ClientTask(
                editTextDestinationIP.getText().toString(),
                Integer.parseInt(editTextDestinationPort.getText().toString()),
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
    public void signBlock(PublicKey linkedPubKey,BlockProto.TrustChainBlock linkedBlock) {
        // do nothing if linked block is not addressed to me
        if(!linkedBlock.getLinkPublicKey().equals(getMyPublicKey())){
            return;
        }
        // do nothing if block is not a request
        if(linkedBlock.getLinkSequenceNumber() != TrustChainBlock.UNKNOWN_SEQ){
            return;
        }
        BlockProto.TrustChainBlock block = createBlock(null,dbHelper.getReadableDatabase(),
                getMyPublicKey(),
                linkedBlock,null);

        sign(block, getMyPublicKey());

        // TODO: validate


        // TODO: log?

        // only if validated correclty
        insertInDB(block,db);
        sendBlock(block);
    }

    /**
     * Builds a half block with the transaction.
     * Reads from database and inserts new halfblock in database.
     * @param transaction
     */
    public void signBlock(byte[] transaction, byte[] linkPubKey) {
        // transaction should be a dictionary TODO: check if this is really needed
        BlockProto.TrustChainBlock block =
                createBlock(transaction,dbHelper.getReadableDatabase(),
                        getMyPublicKey(),null,linkPubKey);
        sign(block,getMyPublicKey());

        // TODO: validate

        // TODO: log?

        // only if validated correctly
        insertInDB(block,db);
        sendBlock(block);
    }


    // Placeholder TODO: change all places where this method gets called to correct method
    public byte[] getMyPublicKey() {
        return EMPTY_PK.toByteArray();
    }

}
