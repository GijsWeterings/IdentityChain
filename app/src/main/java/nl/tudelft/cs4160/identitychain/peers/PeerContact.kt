package nl.tudelft.cs4160.identitychain.peers

import io.realm.Realm
import io.realm.RealmObject

open class PeerContact(var name: String = "", var pk: ByteArray = ByteArray(0)) : RealmObject()

fun nameForContact(realm: Realm, publicKey: ByteArray) =
        realm.where(PeerContact::class.java)
                .equalTo("pk", publicKey)
                .findFirst()?.name