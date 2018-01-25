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
import io.reactivex.disposables.Disposables
import io.reactivex.disposables.SerialDisposable
import io.realm.Realm
import nl.tudelft.cs4160.identitychain.Util.Key
import nl.tudelft.cs4160.identitychain.block.TrustChainBlock
import nl.tudelft.cs4160.identitychain.database.AttestationRequest
import nl.tudelft.cs4160.identitychain.database.PrivateProof
import nl.tudelft.cs4160.identitychain.database.RealmAttestationRequestRepository
import nl.tudelft.cs4160.identitychain.database.TrustChainDBHelper
import nl.tudelft.cs4160.identitychain.grpc.ChainServiceServer
import nl.tudelft.cs4160.identitychain.grpc.asMessage
import nl.tudelft.cs4160.identitychain.grpc.startNetworkOnComputation
import nl.tudelft.cs4160.identitychain.message.ChainService
import nl.tudelft.cs4160.identitychain.peers.PeerConnectionInformation
import nl.tudelft.cs4160.identitychain.network.ServiceFactory
import nl.tudelft.cs4160.identitychain.peers.DiscoveredPeer
import nl.tudelft.cs4160.identitychain.peers.KeyedPeer
import java.net.NetworkInterface
import java.security.KeyPair


class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val selectedPeer: MutableLiveData<KeyedPeer> = MutableLiveData()
    val serviceFactory = initializeServiceFactory()
    val peerSelection: LiveData<KeyedPeer> = selectedPeer
    val keyedPeers: LiveData<KeyedPeer>

    private val grpc: Server
    private val server: ChainServiceServer
    private val dbHelper: TrustChainDBHelper = TrustChainDBHelper(app)

    val realm = Realm.getDefaultInstance()

    val trustedParty = RangeProofTrustedParty()
    val attestationRequestRepository = RealmAttestationRequestRepository()
    val verifyDisposable = SerialDisposable()

    private val _verifyEvents: MutableLiveData<ChainService.PublicSetupResult> = MutableLiveData()
    val verifyEvents: LiveData<ChainService.PublicSetupResult> = _verifyEvents
    val verificationEvents: MutableLiveData<Boolean> = MutableLiveData()


    val kp by lazy(this::initKeys)

    fun select(peer: KeyedPeer) {

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

        val (server, grpc) = ChainServiceServer.createServer(kp, 8080, localIPAddress!!, dbHelper, attestationRequestRepository)
        Log.i(TAG, "created server")
        this.grpc = grpc
        this.server = server
        keyedPeers = LiveDataReactiveStreams.fromPublisher(serviceFactory.startPeerDiscovery().flatMapSingle(server::keyForPeer))
    }

    fun proofSelected(pair: Pair<Int,ChainService.PublicSetupResult>) {
        val (seqNo, block) = pair
        _verifyEvents.value = block

        val peer = selectedPeer.value
        if (peer != null) {
            verifyDisposable.replace(startNetworkOnComputation({ server.verifyExistingBlock(peer.toPeerMessage(), seqNo) })
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({verificationEvents.value = it}, {}))
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

    fun createClaim(a: Int, b: Int, m: Int): Single<ChainService.Empty>? {
        val peeritem = peerSelection.value
        val (public, private) = trustedParty.generateProof(m, a, b)
        val asMessage: ChainService.PublicSetupResult = public.asMessage()
        val publicPayLoad = asMessage.toByteArray()

        return peeritem?.let { server.sendBlockToKnownPeer(it.connectionInformation, publicPayLoad, private) }
    }

    fun startVerificationAndSigning(request: AttestationRequest) {
        Log.i(TAG, "starting verification process")
        val block = ChainService.PeerTrustChainBlock.parseFrom(request.block)
        val delete: (Boolean) -> Unit = {
            if (it) {
                attestationRequestRepository.deleteAttestationRequest(request)
            }
        }
        startNetworkOnComputation { server.signAttestationRequest(block.peer, block.block) }
                .observeOn(AndroidSchedulers.mainThread()).subscribe(delete)
    }

    override fun onCleared() {
        verifyDisposable.dispose()
        grpc.shutdownNow()
        realm.close()
    }

    companion object {
        val TAG = "MainViewModel"
    }
}