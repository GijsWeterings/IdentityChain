package nl.tudelft.cs4160.identitychain.database

import com.zeroknowledgeproof.rangeProof.SetupPrivateResult
import io.realm.Realm
import io.realm.RealmObject
import io.realm.kotlin.delete
import io.realm.kotlin.where
import nl.tudelft.cs4160.identitychain.message.ChainService

interface AttestationRequestRepository {
    fun saveAttestationRequest(peerTrustChainBlock: ChainService.PeerTrustChainBlock)
    fun privateDataForStuff(seq_no: Int): PrivateProof?
    fun savePrivateData(private: SetupPrivateResult, seq_no: Int)
}

class RealmAttestationRequestRepository : AttestationRequestRepository {

    override fun saveAttestationRequest(peerTrustChainBlock: ChainService.PeerTrustChainBlock) {
        Realm.getDefaultInstance().use {
            it.executeTransaction {
                it.copyToRealm(AttestationRequest.fromHalfBlock(peerTrustChainBlock))
            }
        }
    }

    fun deleteAttestationRequest(request: AttestationRequest) {
        Realm.getDefaultInstance().use {
            it.executeTransaction {
                request.deleteFromRealm()
            }
        }

    }

    override fun savePrivateData(private: SetupPrivateResult, seq_no: Int) {
        Realm.getDefaultInstance().use {
            it.executeTransaction {
                it.copyToRealm(PrivateProof.fromPrivateResult(private, seq_no))
            }
        }
    }

    override fun privateDataForStuff(seq_no: Int): PrivateProof? {
        return Realm.getDefaultInstance().use {
            val proof = it.where(PrivateProof::class.java).equalTo("block_no", seq_no).findFirst()
            if (proof != null) {
                it.copyFromRealm(proof)
            } else {
                null
            }
        }
    }
}

class FakeRepository : AttestationRequestRepository {
    val attestationRequests = ArrayList<AttestationRequest>()
    val privateData = ArrayList<PrivateProof>()

    override fun savePrivateData(private: SetupPrivateResult, seq_no: Int) {
        privateData.add(PrivateProof.fromPrivateResult(private, seq_no))
    }

    override fun privateDataForStuff(seq_no: Int): PrivateProof? =
            privateData.find { it.block_no == seq_no }


    override fun saveAttestationRequest(peerTrustChainBlock: ChainService.PeerTrustChainBlock) {
        attestationRequests.add(AttestationRequest.fromHalfBlock(peerTrustChainBlock))
    }
}

open class AttestationRequest : RealmObject() {
    var block: ByteArray = ByteArray(0)
    var peer: Peer? = Peer()

    fun publicKey() = peer?.publicKey

    fun metaDataText(): String {
        val transaction = ChainService.PeerTrustChainBlock.parseFrom(block)
                .block.transaction

        val metaZkp = ChainService.MetaZkp.parseFrom(transaction)

        return "${metaZkp.meta} from: ${metaZkp.zkp.a} to: ${metaZkp.zkp.b}"
    }

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
                ChainService.MetaZkp.parseFrom(block.block.transaction)
                this.block = block.toByteArray()
                peer = realmPeer
            }

        }
    }
}

open class Peer : RealmObject() {
    var hostName: String = ""
    var port: Int = 0
    var publicKey: ByteArray = ByteArray(0)

    companion object {
        fun fromProtoPeer(peer: ChainService.Peer) {

        }
    }
}