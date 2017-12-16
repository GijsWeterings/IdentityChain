package nl.tudelft.cs4160.identitychain.grpc

import io.grpc.ManagedChannelBuilder
import nl.tudelft.cs4160.identitychain.message.ChainGrpc
import nl.tudelft.cs4160.identitychain.message.ChainService
import java.util.concurrent.ConcurrentHashMap

class ChainClientRegistry {
    val registry: ConcurrentHashMap<ChainService.Peer, ChainGrpc.ChainBlockingStub> = ConcurrentHashMap()

    fun findStub(peer: ChainService.Peer) = registry.getOrPut(peer) { channelForPeer(peer) }

    fun channelForPeer(peer: ChainService.Peer): ChainGrpc.ChainBlockingStub {
        val channel = ManagedChannelBuilder.forAddress(peer.hostname, peer.port).usePlaintext(true).build();
        return ChainGrpc.newBlockingStub(channel)
    }
}