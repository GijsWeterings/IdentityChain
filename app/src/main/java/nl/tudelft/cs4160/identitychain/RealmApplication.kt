package nl.tudelft.cs4160.identitychain

import android.app.Application
import io.realm.Realm

class RealmApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Realm.init(this)
    }
}