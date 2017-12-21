package nl.tudelft.cs4160.identitychain.grpc

import io.grpc.ManagedChannelBuilder
import nl.tudelft.cs4160.identitychain.message.ChainGrpc
import nl.tudelft.cs4160.identitychain.message.ChainService
import java.util.concurrent.ConcurrentHashMap

class ChainClientRegistry {
    private val registry: ConcurrentHashMap<ChainService.Peer, ChainGrpc.ChainBlockingStub> = ConcurrentHashMap()
    private val registryAsync: ConcurrentHashMap<ChainService.Peer, ChainGrpc.ChainFutureStub> = ConcurrentHashMap()

    @Deprecated("we should prefer the async operations")
    fun findSyncStub(peer: ChainService.Peer) = registry.getOrPut(peer) { channelForPeer(peer) }
    fun findAsyncStub(peer: ChainService.Peer) = registryAsync.getOrPut(peer) { channelForPeer2(peer) }

    private fun channelForPeer(peer: ChainService.Peer): ChainGrpc.ChainBlockingStub {
        val channel = ManagedChannelBuilder.forAddress(peer.hostname, peer.port).usePlaintext(true).build();
        return ChainGrpc.newBlockingStub(channel)
    }

    private fun channelForPeer2(peer: ChainService.Peer): ChainGrpc.ChainFutureStub {
        val channel = ManagedChannelBuilder.forAddress(peer.hostname, peer.port).usePlaintext(true).build();
        return ChainGrpc.newFutureStub(channel)
    }

}

