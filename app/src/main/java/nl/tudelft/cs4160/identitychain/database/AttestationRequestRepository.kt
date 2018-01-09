package nl.tudelft.cs4160.identitychain.database

import io.realm.Realm
import io.realm.RealmObject
import nl.tudelft.cs4160.identitychain.message.ChainService

class AttestationRequestRepository(val realm: Realm) {

    fun saveAttestationRequest(publicKey: ByteArray, zkp: ChainService.PublicSetupResult) {
        val serializedZkp = zkp.toByteArray()
        val request = realm.createObject(AttestationRequest::class.java)

        realm.executeTransaction {
            request.publicKey = publicKey
            request.zkp = serializedZkp
        }
    }
}


open class AttestationRequest() : RealmObject() {
    var publicKey: ByteArray = ByteArray(0)
    var zkp: ByteArray = ByteArray(0)
}