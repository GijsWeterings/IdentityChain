package nl.tudelft.cs4160.identitychain.grpc

import io.grpc.Channel
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.ClientResponseObserver
import io.grpc.stub.StreamObserver
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import nl.tudelft.cs4160.identitychain.message.ChainGrpc
import nl.tudelft.cs4160.identitychain.message.ChainService
import nl.tudelft.cs4160.identitychain.message.MessageProto
import java.util.concurrent.ConcurrentHashMap

class ChainClientRegistry {
    val registry: ConcurrentHashMap<ChainService.Peer, ChainGrpc.ChainBlockingStub> = ConcurrentHashMap()

    fun findStub(peer: ChainService.Peer) = registry.getOrPut(peer) { channelForPeer(peer) }

    fun channelForPeer(peer: ChainService.Peer): ChainGrpc.ChainBlockingStub {
        val channel = ManagedChannelBuilder.forAddress(peer.hostname, peer.port).usePlaintext(true).build();
        return ChainGrpc.newBlockingStub(channel)
    }

    fun channelForPeer2(peer: ChainService.Peer): ChainGrpc.ChainStub {
        val channel = ManagedChannelBuilder.forAddress(peer.hostname, peer.port).usePlaintext(true).build();
        return ChainGrpc.newStub(channel)
    }

    fun foo(peer: ChainService.Peer, request: ChainService.PeerCrawlRequest): Flowable<ChainService.PeerTrustChainBlock> {
        return Flowable.create({ source ->
            channelForPeer2(peer).recieveCrawlRequest(request, object : StreamObserver<ChainService.PeerTrustChainBlock> {
                override fun onCompleted() {
                    source.onComplete()
                }

                override fun onError(t: Throwable) {
                    source.onError(t)
                }

                override fun onNext(value: ChainService.PeerTrustChainBlock) {
                    source.onNext(value)
                }

            })
        }, BackpressureStrategy.BUFFER)
    }
}

