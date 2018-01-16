package nl.tudelft.cs4160.identitychain

import android.app.Application
import io.realm.Realm
import io.realm.RealmConfiguration

class RealmApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Realm.init(this)
        Realm.setDefaultConfiguration(RealmConfiguration.Builder().deleteRealmIfMigrationNeeded().build())
    }
}