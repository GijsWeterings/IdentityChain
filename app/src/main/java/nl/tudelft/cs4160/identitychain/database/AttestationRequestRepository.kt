package nl.tudelft.cs4160.identitychain.database

import io.realm.Realm
import io.realm.RealmObject
import io.realm.kotlin.delete
import nl.tudelft.cs4160.identitychain.message.ChainService

interface AttestationRequestRepository {
    fun saveAttestationRequest(peerTrustChainBlock: ChainService.PeerTrustChainBlock)
}

class RealmAttestationRequestRepository : AttestationRequestRepository {

    override fun saveAttestationRequest(peerTrustChainBlock: ChainService.PeerTrustChainBlock) {
        val realm = Realm.getDefaultInstance()

        realm.executeTransaction {
            it.copyToRealm(AttestationRequest.fromHalfBlock(peerTrustChainBlock))
        }

        realm.close()
    }

    fun deleteAttestationRequest(request: AttestationRequest) {
        val realm = Realm.getDefaultInstance()
        realm.executeTransaction {
            request.deleteFromRealm()
        }

        realm.close()
    }
}

class FakeRepository : AttestationRequestRepository {
    val attestationRequests = ArrayList<AttestationRequest>()
    override fun saveAttestationRequest(peerTrustChainBlock: ChainService.PeerTrustChainBlock) {
        attestationRequests.add(AttestationRequest.fromHalfBlock(peerTrustChainBlock))
    }
}

open class AttestationRequest : RealmObject() {
    var block: ByteArray = ByteArray(0)
    var peer: Peer? = Peer()

    fun publicKey() = peer?.publicKey

    companion object {
        fun fromHalfBlock(block: ChainService.PeerTrustChainBlock): AttestationRequest {
            val realmPeer = Peer().apply {
                val peer = block.peer
                hostName = peer.hostname
                port = peer.port
                publicKey = peer.publicKey.toByteArray()
            }

            return AttestationRequest().apply {
                //verify that the transaction is indeed a public result, but store it as bytes.
                ChainService.PublicSetupResult.parseFrom(block.block.transaction)
                this.block = block.toByteArray()
                peer = realmPeer
            }

        }
    }
}

open class Peer() : RealmObject() {
    var hostName: String = ""
    var port: Int = 0
    var publicKey: ByteArray = ByteArray(0)

    companion object {
        fun fromProtoPeer(peer: ChainService.Peer) {

        }
    }
}