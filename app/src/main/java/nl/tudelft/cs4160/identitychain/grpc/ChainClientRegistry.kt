package nl.tudelft.cs4160.identitychain.grpc

import io.grpc.ManagedChannelBuilder
import nl.tudelft.cs4160.identitychain.message.ChainGrpc
import nl.tudelft.cs4160.identitychain.message.ChainService
import java.util.concurrent.ConcurrentHashMap

class ChainClientRegistry {
    private val registryAsync: ConcurrentHashMap<ChainService.Peer, ChainGrpc.ChainFutureStub> = ConcurrentHashMap()
    fun findStub(peer: ChainService.Peer) = registryAsync.getOrPut(peer) { channelForPeer(peer) }

    private fun channelForPeer(peer: ChainService.Peer): ChainGrpc.ChainFutureStub {
        val channel = ManagedChannelBuilder.forAddress(peer.hostname, peer.port).usePlaintext(true).build();
        return ChainGrpc.newFutureStub(channel)
    }

}

