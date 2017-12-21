package nl.tudelft.cs4160.identitychain.main

import android.app.AlertDialog
import android.arch.lifecycle.ViewModelProviders
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.zeroknowledgeproof.rangeProof.RangeProofTrustedParty
import io.grpc.Server
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.main_screen.*
import kotlinx.android.synthetic.main.main_screen.view.*
import nl.tudelft.cs4160.identitychain.R
import nl.tudelft.cs4160.identitychain.Util.Key
import nl.tudelft.cs4160.identitychain.block.TrustChainBlock
import nl.tudelft.cs4160.identitychain.database.TrustChainDBHelper
import nl.tudelft.cs4160.identitychain.grpc.ChainServiceServer
import nl.tudelft.cs4160.identitychain.grpc.asMessage
import nl.tudelft.cs4160.identitychain.message.ChainService
import java.net.NetworkInterface
import java.security.KeyPair

class MainFragment : Fragment() {
    private lateinit var grpc: Server
    lateinit private var server: ChainServiceServer
    private lateinit var dbHelper: TrustChainDBHelper
    private lateinit var viewModel: MainViewModel

    val kp by lazy(this::initKeys)

    val trustedParty = RangeProofTrustedParty()
    val zkp = trustedParty.generateProof(30, 18, 100)

    internal var debugMenuListener: View.OnLongClickListener = View.OnLongClickListener {
        val intent = Intent(this.activity, DebugActivity::class.java)
        startActivity(intent)
        true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.main_screen, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProviders.of(activity).get(MainViewModel::class.java)
        init()

        view.addClaimButton.setOnClickListener {
            val peeritem = viewModel.peerSelection.value
            if (peeritem == null) {
                Toast.makeText(activity, "Please select a peer to connect with", Toast.LENGTH_SHORT).show()
            } else {
                val asMessage: ChainService.PublicSetupResult = zkp.first.asMessage()
                val publicPayLoad = asMessage.toByteArray()
                server.sendBlockToKnownPeer(peeritem, publicPayLoad).subscribe()
            }
        }
    }


    fun init() {
        dbHelper = TrustChainDBHelper(this.activity)
        imageView.setOnLongClickListener(debugMenuListener)


        if (isStartedFirstTime) {
            val block = TrustChainBlock.createGenesisBlock(kp)
            dbHelper.insertInDB(block)
        }

        val (server, grpc) = ChainServiceServer.createServer(kp, 8080, localIPAddress!!, dbHelper, this::attestationPrompt, zkp.second)
        this.grpc = grpc
        this.server = server

    }


    private fun initKeys(): KeyPair {
        val kp = Key.loadKeys(this.activity.applicationContext)
        if (kp == null) {
            val kp = Key.createAndSaveKeys(this.activity.applicationContext)
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


    private fun attestationPrompt(attestation: ChainService.PublicSetupResult): Single<Boolean> {
        val fuckyiou = Single.create<Boolean> { source ->
            val message = "attest for some cool dude that his _ is between ${attestation.a} - ${attestation.b}"
            AlertDialog.Builder(this.activity).setMessage(message)
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

    /**
     * Checks if this is the first time the app is started and returns a boolean value indicating
     * this state.
     * @return state - false if the app has been initialized before, true if first time app started
     */
    // check if a genesis block is present in database
    val isStartedFirstTime: Boolean
        get() {
            val genesisBlock = dbHelper.getBlock(kp.public.encoded, TrustChainBlock.GENESIS_SEQ)

            return genesisBlock == null
        }


    override fun onDestroy() {
        super.onDestroy()
        grpc.shutdownNow()
    }

    companion object {
        private val TAG = MainFragment::class.java.toString()
    }
}