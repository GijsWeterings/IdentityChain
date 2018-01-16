package nl.tudelft.cs4160.identitychain.main

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
import nl.tudelft.cs4160.identitychain.database.AttestationRequest
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

    private val grpc: Server
    private val server: ChainServiceServer
    private val dbHelper: TrustChainDBHelper = TrustChainDBHelper(app)

    val trustedParty = RangeProofTrustedParty()
    val zkp = trustedParty.generateProof(30, 18, 100)
    val attestationRequestRepository = RealmAttestationRequestRepository()

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

        val (server, grpc) = ChainServiceServer.createServer(kp, 8080, localIPAddress!!, dbHelper, zkp.second, attestationRequestRepository)
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

    fun createClaim(): Single<ChainService.Empty>? {
        val peeritem = peerSelection.value
        val asMessage: ChainService.PublicSetupResult = zkp.first.asMessage()
        val publicPayLoad = asMessage.toByteArray()
        return peeritem?.let { server.sendBlockToKnownPeer(it, publicPayLoad) }
    }

    fun startVerificationAndSigning(request: AttestationRequest) {
        Log.i(TAG,"starting verification process")
        val block = ChainService.PeerTrustChainBlock.parseFrom(request.block)
        val delete: (Boolean) -> Unit = {
            if (it) {
                attestationRequestRepository.deleteAttestationRequest(request)
            }
        }
        server.signAttestationRequest(block.peer, block.block)
                .observeOn(AndroidSchedulers.mainThread()).subscribe(delete)

    }

    override fun onCleared() {
        grpc.shutdownNow()
    }

    companion object {
        val TAG = "MainViewModel"
    }
}