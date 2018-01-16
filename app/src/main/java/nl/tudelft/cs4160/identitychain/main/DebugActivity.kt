package nl.tudelft.cs4160.identitychain.main

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_debug.*
import nl.tudelft.cs4160.identitychain.R
import nl.tudelft.cs4160.identitychain.chainExplorer.ChainExplorerActivity
import nl.tudelft.cs4160.identitychain.modals.BiometricAuthenticationFragment

class DebugActivity(): AppCompatActivity() {

    internal var chainExplorerButtonListener: View.OnClickListener = View.OnClickListener {
        val intent = Intent(this, ChainExplorerActivity::class.java)
        startActivity(intent)
    }

    internal var fingerprintButtonListener: View.OnClickListener = View.OnClickListener {
        val intent = Intent(this, BiometricAuthenticationFragment::class.java)
        startActivity(intent)
    }

    internal var resetDatabaseListener: View.OnClickListener = View.OnClickListener {
        if (Build.VERSION_CODES.KITKAT <= Build.VERSION.SDK_INT) {
            (applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
                    .clearApplicationUserData()
        } else {
            Toast.makeText(applicationContext, "Requires at least API 19 (KitKat)", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug)

        chainExplorerButton.setOnClickListener(chainExplorerButtonListener)
        resetDatabaseButton.setOnClickListener(resetDatabaseListener)
        debugFingerprintButton.setOnClickListener(fingerprintButtonListener)
    }
}