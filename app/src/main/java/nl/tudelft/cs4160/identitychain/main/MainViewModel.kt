package nl.tudelft.cs4160.identitychain.main

import android.app.AlertDialog
import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.LiveDataReactiveStreams
import android.arch.lifecycle.MutableLiveData
import android.util.Log
import com.zeroknowledgeproof.rangeProof.RangeProofTrustedParty
import io.grpc.Server
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.realm.Realm
import nl.tudelft.cs4160.identitychain.Util.Key
import nl.tudelft.cs4160.identitychain.block.TrustChainBlock
import nl.tudelft.cs4160.identitychain.database.AttestationRequestRepository
import nl.tudelft.cs4160.identitychain.database.RealmAttestationRequestRepository
import nl.tudelft.cs4160.identitychain.database.TrustChainDBHelper
import nl.tudelft.cs4160.identitychain.grpc.ChainServiceServer
import nl.tudelft.cs4160.identitychain.grpc.asMessage
import nl.tudelft.cs4160.identitychain.message.ChainService
import nl.tudelft.cs4160.identitychain.network.PeerItem
import nl.tudelft.cs4160.identitychain.network.ServiceFactory
import java.net.NetworkInterface
import java.security.KeyPair


class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val selectedPeer: MutableLiveData<PeerItem> = MutableLiveData()
    val serviceFactory = initializeServiceFactory()
    val allPeers: LiveData<PeerItem> = LiveDataReactiveStreams.fromPublisher(serviceFactory.startPeerDiscovery())
    val peerSelection: LiveData<PeerItem> = selectedPeer

    private lateinit var grpc: Server
    lateinit private var server: ChainServiceServer
    private val dbHelper: TrustChainDBHelper = TrustChainDBHelper(app)

    val trustedParty = RangeProofTrustedParty()
    val zkp = trustedParty.generateProof(30, 18, 100)

    val kp by lazy(this::initKeys)


    fun select(peer: PeerItem) {
        selectedPeer.value = peer
    }

    fun initializeServiceFactory(): ServiceFactory {
        val factory = ServiceFactory(getApplication())
        factory.initializeDiscoveryServer()
        return factory
    }

    init {
        if (isStartedFirstTime) {
            val block = TrustChainBlock.createGenesisBlock(kp)
            dbHelper.insertInDB(block)
        }

        val realm = Realm.getDefaultInstance()
        val (server, grpc) = ChainServiceServer.createServer(kp, 8080, localIPAddress!!, dbHelper, this::attestationPrompt, zkp.second, RealmAttestationRequestRepository(realm))
        Log.i(TAG, "created server")
        this.grpc = grpc
        this.server = server

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

    private fun initKeys(): KeyPair {
        val kp = Key.loadKeys(getApplication())
        if (kp == null) {
            val kp = Key.createAndSaveKeys(getApplication())
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
        val observer = Single.create<Boolean> { source ->
            val message = "attest for some cool dude that his _ is between ${attestation.a} - ${attestation.b}"
            AlertDialog.Builder(getApplication()).setMessage(message)
                    .setPositiveButton("yes") { _, _ -> source.onSuccess(true) }
                    .setNegativeButton("no") { _, _ -> source.onSuccess(false) }.show()
        }
        return observer.subscribeOn(AndroidSchedulers.mainThread())
    }

    fun createClaim(): Single<ChainService.Empty>? {
        val peeritem = peerSelection.value
        val asMessage: ChainService.PublicSetupResult = zkp.first.asMessage()
        val publicPayLoad = asMessage.toByteArray()
        return peeritem?.let { server.sendBlockToKnownPeer(it, publicPayLoad) }
    }

    override fun onCleared() {
        grpc.shutdownNow()
    }

    companion object {
        val TAG = "MainViewModel"
    }
}