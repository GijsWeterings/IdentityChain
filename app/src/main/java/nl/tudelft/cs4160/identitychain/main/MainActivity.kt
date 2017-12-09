package nl.tudelft.cs4160.identitychain.main

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_main.*
import nl.tudelft.cs4160.identitychain.R
import nl.tudelft.cs4160.identitychain.Util.Key
import nl.tudelft.cs4160.identitychain.block.TrustChainBlock
import nl.tudelft.cs4160.identitychain.block.TrustChainBlock.GENESIS_SEQ
import nl.tudelft.cs4160.identitychain.chainExplorer.ChainExplorerActivity
import nl.tudelft.cs4160.identitychain.connection.Communication
import nl.tudelft.cs4160.identitychain.connection.CommunicationListener
import nl.tudelft.cs4160.identitychain.connection.network.NetworkCommunication
import nl.tudelft.cs4160.identitychain.database.TrustChainDBHelper
import nl.tudelft.cs4160.identitychain.main.bluetooth.BluetoothActivity
import nl.tudelft.cs4160.identitychain.network.PeerViewRecyclerAdapter
import nl.tudelft.cs4160.identitychain.network.ServiceFactory
import java.net.NetworkInterface
import java.security.KeyPair

class MainActivity : AppCompatActivity(), CommunicationListener {
    lateinit internal var dbHelper: TrustChainDBHelper

    private var communication: Communication? = null



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
    internal var connectionButtonListener: View.OnClickListener = View.OnClickListener {
        //TODO PUT THIS BACK
//        val peer = Peer(null, destinationIPText.text.toString(),
//                Integer.parseInt(destinationPortText.text.toString()))
        //send either a crawl request or a half block
//        communication!!.connectToPeer(peer)
    }

    internal var chainExplorerButtonListener: View.OnClickListener = View.OnClickListener {
        val intent = Intent(this, ChainExplorerActivity::class.java)
        startActivity(intent)
    }

    internal var keyOptionsListener: View.OnClickListener = View.OnClickListener {
        val intent = Intent(this, BluetoothActivity::class.java)
        startActivity(intent)
    }

    internal var resetDatabaseListener: View.OnClickListener = View.OnClickListener {
        if (Build.VERSION_CODES.KITKAT <= Build.VERSION.SDK_INT) {
            (applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
                    .clearApplicationUserData()
        } else {
            Toast.makeText(applicationContext, "Requires at least API 19 (KitKat)", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Checks if this is the first time the app is started and returns a boolean value indicating
     * this state.
     * @return state - false if the app has been initialized before, true if first time app started
     */
    // check if a genesis block is present in database
    val isStartedFirstTime: Boolean
        get() {
            val genesisBlock = dbHelper.getBlock(kp!!.public.encoded, GENESIS_SEQ)

            return genesisBlock == null
        }

    /**
     * Finds the local IP address of this device, loops trough network interfaces in order to find it.
     * The address that is not a loopback address is the IP of the device.
     * @return a string representation of the device's IP address
     */
    val localIPAddress: String?
        get() {
            try {
                val netInterfaces = NetworkInterface.getNetworkInterfaces()
                for (netInt in netInterfaces) {
                    for (addr in netInt.inetAddresses) {
                        if (addr.isSiteLocalAddress) {
                            return addr.hostAddress
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val serviceFactory = ServiceFactory(this)
        initVariables()
        init(serviceFactory)

        serviceFactory.initializeDiscoveryServer()
    }

    private fun initVariables() {
        statusText.movementMethod = ScrollingMovementMethod()
    }

    private fun init(serviceFactory: ServiceFactory) {
        dbHelper = TrustChainDBHelper(this)


        //create or load keys
        initKeys()

        if (isStartedFirstTime) {
            val block = TrustChainBlock.createGenesisBlock(kp)
            dbHelper.insertInDB(block)
        }

        communication = NetworkCommunication(dbHelper, kp, this)

        connectionButton.setOnClickListener(connectionButtonListener)
        chainExplorerButton.setOnClickListener(chainExplorerButtonListener)
        bluetoothButton.setOnClickListener(keyOptionsListener)
        resetDatabaseButton.setOnClickListener(resetDatabaseListener)

        discoveryList.layoutManager = LinearLayoutManager(this)
        val peerViewRecyclerAdapter = PeerViewRecyclerAdapter()
        discoveryList.adapter = peerViewRecyclerAdapter

        serviceFactory.startPeerDiscovery()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(peerViewRecyclerAdapter::addItem)


        //start listening for messages
        communication!!.start()

    }

    private fun initKeys() {
        kp = Key.loadKeys(applicationContext)
        if (kp == null) {
            kp = Key.createAndSaveKeys(applicationContext)
            Log.i(TAG, "New keys created")
        }
    }



    override fun updateLog(msg: String) {
        //just to be sure run it on the ui thread
        //this is not necessary when this function is called from a AsyncTask
        runOnUiThread {
            val statusText = findViewById<TextView>(R.id.statusText)
            statusText.append(msg)
        }
    }

    companion object {
        val TRANSACTION = "Hello world!"
        private val TAG = MainActivity::class.java.toString()

        /**
         * Key pair of user
         */
        internal var kp: KeyPair? = null
    }
}
