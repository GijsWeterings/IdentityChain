package nl.tudelft.cs4160.trustchain_android.main

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import nl.tudelft.cs4160.trustchain_android.Peer
import nl.tudelft.cs4160.trustchain_android.R
import nl.tudelft.cs4160.trustchain_android.Util.Key
import nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock
import nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock.GENESIS_SEQ
import nl.tudelft.cs4160.trustchain_android.chainExplorer.ChainExplorerActivity
import nl.tudelft.cs4160.trustchain_android.connection.Communication
import nl.tudelft.cs4160.trustchain_android.connection.CommunicationListener
import nl.tudelft.cs4160.trustchain_android.connection.network.NetworkCommunication
import nl.tudelft.cs4160.trustchain_android.database.TrustChainDBHelper
import nl.tudelft.cs4160.trustchain_android.main.bluetooth.BluetoothActivity
import java.net.NetworkInterface
import java.security.KeyPair
import java.util.*

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
        val peer = Peer(null, destinationIPText.text.toString(),
                Integer.parseInt(destinationPortText.text.toString()))
        //send either a crawl request or a half block
        communication!!.connectToPeer(peer)
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

        initVariables()
        init()
    }

    private fun initVariables() {
        statusText.movementMethod = ScrollingMovementMethod()
    }

    private fun init() {
        dbHelper = TrustChainDBHelper(this)


        //create or load keys
        initKeys()

        if (isStartedFirstTime) {
            val block = TrustChainBlock.createGenesisBlock(kp)
            dbHelper.insertInDB(block)
        }

        communication = NetworkCommunication(dbHelper, kp, this)

        updateIP()
        updateLocalIPField(localIPAddress)

        connectionButton.setOnClickListener(connectionButtonListener)
        chainExplorerButton.setOnClickListener(chainExplorerButtonListener)
        bluetoothButton.setOnClickListener(keyOptionsListener)
        resetDatabaseButton.setOnClickListener(resetDatabaseListener)

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

    /**
     * Updates the external IP address textfield to the given IP address.
     */
    fun updateExternalIPField(ipAddress: String) {
        externalIPText.text = ipAddress
        Log.i(TAG, "Updated external IP Address: " + ipAddress)
    }

    /**
     * Updates the internal IP address textfield to the given IP address.
     */
    fun updateLocalIPField(ipAddress: String?) {
        localIPText.text = ipAddress
        Log.i(TAG, "Updated local IP Address:" + ipAddress!!)
    }

    /**
     * Finds the external IP address of this device by making an API call to https://www.ipify.org/.
     * The networking runs on a separate thread.
     */
    fun updateIP() {
        val thread = Thread(Runnable {
            try {
                java.util.Scanner(java.net.URL("https://api.ipify.org").openStream(), "UTF-8").useDelimiter("\\A").use { s ->
                    val ip = s.next()
                    // new thread to handle UI updates
                    this@MainActivity.runOnUiThread { updateExternalIPField(ip) }
                }
            } catch (e: java.io.IOException) {
                e.printStackTrace()
            }
        })
        thread.start()
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
