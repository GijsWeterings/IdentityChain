package nl.tudelft.cs4160.identitychain.peers

import com.google.protobuf.ByteString
import nl.tudelft.cs4160.identitychain.message.ChainService

/**
 * The minimal amount of information required to start a grpc connection to a peer.
 * The port, for now hard coded since every grpc server runs on 8080
 */
data class PeerConnectionInformation(val host: String, val port: Int = 8080)

fun ChainService.Peer.toPeerConnectionInformation() = PeerConnectionInformation(this.hostname, this.port)

/**
 * A peer that has just been discovered through @see nl.tudelft.cs4160.identitychain.network.ServiceFactory
 * This holds on to the discovery name in case no name has been associated yet.
 */
data class DiscoveredPeer(val connectionInformation: PeerConnectionInformation, val name: String)

/**
 * A peer whose public key has been resolved through an rpc request.
 *
 * The public key is used to persistently represent the identity of the peer.
 */
class KeyedPeer(val connectionInformation: PeerConnectionInformation, val publicKey: ByteArray, val name: String, val port: Int = 8080) {

    val host: String
        get() = connectionInformation.host

    fun toPeerMessage() = ChainService.Peer.newBuilder()
            .setHostname(host)
            .setPort(port)
            .setPublicKey(ByteString.copyFrom(publicKey))
            .build()
}


fun addKeyToPeer(peerItem: DiscoveredPeer, publicKey: ByteArray) = with(peerItem) { KeyedPeer(peerItem.connectionInformation, publicKey, peerItem.name) }
