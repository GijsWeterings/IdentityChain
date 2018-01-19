package nl.tudelft.cs4160.identitychain.peers

import io.realm.RealmObject

open class PeerContact : RealmObject() {
    var name: String = ""
    var pk: ByteArray = ByteArray(0)
}