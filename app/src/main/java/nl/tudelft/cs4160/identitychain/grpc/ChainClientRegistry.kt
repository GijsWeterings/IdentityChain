package nl.tudelft.cs4160.identitychain.grpc

import io.grpc.ManagedChannelBuilder
import nl.tudelft.cs4160.identitychain.message.ChainGrpc
import nl.tudelft.cs4160.identitychain.message.ChainService
import nl.tudelft.cs4160.identitychain.peers.PeerConnectionInformation
import java.util.concurrent.ConcurrentHashMap

class ChainClientRegistry {
    private val registryAsync: ConcurrentHashMap<PeerConnectionInformation, ChainGrpc.ChainFutureStub> = ConcurrentHashMap()
    fun findStub(peer: PeerConnectionInformation) = registryAsync.getOrPut(peer) { channelForPeer(peer) }

    private fun channelForPeer(peer: PeerConnectionInformation): ChainGrpc.ChainFutureStub {
        val channel = ManagedChannelBuilder.forAddress(peer.host, peer.port).usePlaintext(true).build();
        return ChainGrpc.newFutureStub(channel)
    }

}

