package nl.tudelft.cs4160.identitychain.main


import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.zeroknowledgeproof.rangeProof.RangeProofTrustedParty
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_main.*
import nl.tudelft.cs4160.identitychain.Peer
import nl.tudelft.cs4160.identitychain.R
import nl.tudelft.cs4160.identitychain.Util.Key
import nl.tudelft.cs4160.identitychain.block.TrustChainBlock
import nl.tudelft.cs4160.identitychain.block.TrustChainBlock.GENESIS_SEQ
import nl.tudelft.cs4160.identitychain.connection.CommunicationListener
import nl.tudelft.cs4160.identitychain.database.TrustChainDBHelper
import nl.tudelft.cs4160.identitychain.grpc.ChainServiceServer
import nl.tudelft.cs4160.identitychain.grpc.asMessage
import nl.tudelft.cs4160.identitychain.message.ChainService
import nl.tudelft.cs4160.identitychain.modals.PeerConnectActivity
import nl.tudelft.cs4160.identitychain.network.PeerViewRecyclerAdapter
import nl.tudelft.cs4160.identitychain.network.ServiceFactory
import java.net.NetworkInterface
import java.security.KeyPair


class MainActivity : AppCompatActivity(), CommunicationListener {
    lateinit internal var dbHelper: TrustChainDBHelper

    val kp by lazy(this::initKeys)
    lateinit private var server: ChainServiceServer

    var peerViewRecyclerAdapter = PeerViewRecyclerAdapter()
    val trustedParty = RangeProofTrustedParty()
    val zkp = trustedParty.generateProof(30, 18, 100)


    internal var connectToDeviceButtonListener: View.OnClickListener = View.OnClickListener {
        val dialog = PeerConnectActivity()
        dialog.discoveryListAdapter = peerViewRecyclerAdapter
        dialog.retainInstance = true
        val ft = fragmentManager.beginTransaction()
        dialog.show(ft, PeerConnectActivity.TAG)

    }

    internal var debugMenuListener: View.OnLongClickListener = View.OnLongClickListener {
        val intent = Intent(this, DebugActivity::class.java)
        startActivity(intent)
        true
    }

    /**
     * Checks if this is the first time the app is started and returns a boolean value indicating
     * this state.
     * @return state - false if the app has been initialized before, true if first time app started
     */
    // check if a genesis block is present in database
    val isStartedFirstTime: Boolean
        get() {
            val genesisBlock = dbHelper.getBlock(kp.public.encoded, GENESIS_SEQ)

            return genesisBlock == null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val serviceFactory = ServiceFactory(this)
//        initVariables()
        init(serviceFactory)

        serviceFactory.initializeDiscoveryServer()

        addClaimButton.setOnClickListener {
            val peeritem = peerViewRecyclerAdapter.getItem(0).withPort(8080)
            val asMessage: ChainService.PublicSetupResult = zkp.first.asMessage()
            val publicPayLoad = asMessage.toByteArray()
            server.sendBlockToKnownPeer(peeritem, publicPayLoad)
        }
    }

//    private fun initVariables() {
//        statusText.movementMethod = ScrollingMovementMethod()
//    }

    private fun init(serviceFactory: ServiceFactory) {
        dbHelper = TrustChainDBHelper(this)

        if (isStartedFirstTime) {
            val block = TrustChainBlock.createGenesisBlock(kp)
            dbHelper.insertInDB(block)
        }
        imageView.setOnLongClickListener(debugMenuListener)
        connectToDeviceButton.setOnClickListener(connectToDeviceButtonListener)

        serviceFactory.startPeerDiscovery()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(peerViewRecyclerAdapter::addItem)


        //start listening for messages
        requireNotNull(localIPAddress, { "error could not find local IP" })


        val (server, grpc) = ChainServiceServer.createServer(kp, 8080, localIPAddress!!, dbHelper, this::attestationPrompt, zkp.second)

//        val (server, grpc) = ChainServiceServer.createServer(kp, 8080, localIPAddress!!, TrustChainMemoryStorage(kp), this::attestationPrompt, zkp.second)
        this.server = server

    }

    private fun attestationPrompt(attestation: ChainService.PublicSetupResult): Single<Boolean> {
        val fuckyiou = Single.create<Boolean> { source ->
            val message = "attest for some cool dude that his _ is between ${attestation.a} - ${attestation.b}"
            AlertDialog.Builder(this).setMessage(message)
                    .setPositiveButton("yes", object : DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface?, which: Int) {
                            source.onSuccess(true)
                        }
                    })
                    .setNegativeButton("no", object : DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface?, which: Int) {
                            source.onSuccess(false)
                        }
                    }).show()
        }
        return fuckyiou.subscribeOn(AndroidSchedulers.mainThread())
    }

    private fun initKeys(): KeyPair {
        val kp = Key.loadKeys(applicationContext)
        if (kp == null) {
            val kp = Key.createAndSaveKeys(applicationContext)
            Log.i(TAG, "New keys created")
            return kp
        }
        return kp
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


    override fun updateLog(msg: String) {
        //just to be sure run it on the ui thread
        //this is not necessary when this function is called from a AsyncTask
        runOnUiThread {
//            val statusText = findViewById<TextView>(R.id.statusText)
//            statusText.append(msg)
        }
    }

    companion object {
        private val TAG = MainActivity::class.java.toString()
    }
}
