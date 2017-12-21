package nl.tudelft.cs4160.identitychain.main

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.LiveDataReactiveStreams
import android.arch.lifecycle.MutableLiveData
import nl.tudelft.cs4160.identitychain.network.PeerItem
import nl.tudelft.cs4160.identitychain.network.ServiceFactory


class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val selectedPeer: MutableLiveData<PeerItem> = MutableLiveData()
    val serviceFactory = initializeServiceFactory()
    val allPeers: LiveData<PeerItem> = LiveDataReactiveStreams.fromPublisher(serviceFactory.startPeerDiscovery())
    val peerSelection: LiveData<PeerItem> = selectedPeer

    fun select(peer: PeerItem) {
        selectedPeer.value = peer
    }

    fun initializeServiceFactory(): ServiceFactory {
        val factory = ServiceFactory(getApplication())
        factory.initializeDiscoveryServer()
        return factory
    }


}