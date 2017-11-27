package nl.tudelft.cs4160.trustchain_android.main;

import android.app.ActivityManager;
import android.content.Intent;
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

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.security.KeyPair;
import java.util.Collections;
import java.util.List;

import nl.tudelft.cs4160.trustchain_android.Peer;
import nl.tudelft.cs4160.trustchain_android.R;
import nl.tudelft.cs4160.trustchain_android.Util.Key;
import nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock;
import nl.tudelft.cs4160.trustchain_android.chainExplorer.ChainExplorerActivity;
import nl.tudelft.cs4160.trustchain_android.connection.Communication;
import nl.tudelft.cs4160.trustchain_android.connection.CommunicationListener;
import nl.tudelft.cs4160.trustchain_android.connection.network.NetworkCommunication;
import nl.tudelft.cs4160.trustchain_android.database.TrustChainDBHelper;
import nl.tudelft.cs4160.trustchain_android.main.bluetooth.BluetoothActivity;
import nl.tudelft.cs4160.trustchain_android.message.MessageProto;

import static nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock.GENESIS_SEQ;

public class MainActivity extends AppCompatActivity implements CommunicationListener {


    public final static String TRANSACTION = "Hello world!";
    private final static String TAG = MainActivity.class.toString();

    TrustChainDBHelper dbHelper;

    TextView externalIPText;
    TextView localIPText;
    TextView statusText;
    Button connectionButton;
    Button chainExplorerButton;
    Button resetDatabaseButton;
    Button bluetoothButton;
    EditText editTextDestinationIP;
    EditText editTextDestinationPort;

    MainActivity thisActivity;

    private Communication communication;

    /**
     * Key pair of user
     */
    static KeyPair kp;

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
            Peer peer = new Peer(null, editTextDestinationIP.getText().toString(),
                    Integer.parseInt(editTextDestinationPort.getText().toString()));
            //send either a crawl request or a half block
            communication.connectToPeer(peer);
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
            Intent intent = new Intent(thisActivity, BluetoothActivity.class);
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
        localIPText = findViewById(R.id.my_local_ip);
        externalIPText = findViewById(R.id.my_external_ip);
        statusText = findViewById(R.id.status);
        statusText.setMovementMethod(new ScrollingMovementMethod());
        editTextDestinationIP = findViewById(R.id.destination_IP);
        editTextDestinationPort = findViewById(R.id.destination_port);
        connectionButton = findViewById(R.id.connection_button);
        chainExplorerButton = findViewById(R.id.chain_explorer_button);
        resetDatabaseButton = findViewById(R.id.reset_database_button);
        bluetoothButton = findViewById(R.id.bluetooth_connection_button);
    }

    private void init() {
        dbHelper = new TrustChainDBHelper(thisActivity);


        //create or load keys
        initKeys();

        if(isStartedFirstTime()) {
            MessageProto.TrustChainBlock block = TrustChainBlock.createGenesisBlock(kp);
            dbHelper.insertInDB(block);
        }

        communication = new NetworkCommunication(dbHelper, kp, this);

        updateIP();
        updateLocalIPField(getLocalIPAddress());

        connectionButton.setOnClickListener(connectionButtonListener);
        chainExplorerButton.setOnClickListener(chainExplorerButtonListener);
        bluetoothButton.setOnClickListener(keyOptionsListener);
        resetDatabaseButton.setOnClickListener(resetDatabaseListener);

        //start listening for messages
        communication.start();

    }

    private void initKeys() {
        kp = Key.loadKeys(getApplicationContext());
        if(kp == null) {
            kp = Key.createAndSaveKeys(getApplicationContext());
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
        MessageProto.TrustChainBlock genesisBlock = dbHelper.getBlock(kp.getPublic().getEncoded(),GENESIS_SEQ);

        return genesisBlock == null;
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


    @Override
    public void updateLog(final String msg) {
        //just to be sure run it on the ui thread
        //this is not necessary when this function is called from a AsyncTask
        runOnUiThread(new Runnable() {
                  @Override
                  public void run() {
                      TextView statusText = findViewById(R.id.status);
                      statusText.append(msg);
                  }
              });
    }
}
