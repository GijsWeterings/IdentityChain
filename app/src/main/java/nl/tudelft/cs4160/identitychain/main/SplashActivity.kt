package nl.tudelft.cs4160.identitychain.main

import android.Manifest
import android.support.v7.app.AppCompatActivity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import nl.tudelft.cs4160.identitychain.R


class SplashActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback {

    // Splash screen timer
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val needContactPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED
        val needFingerPrintPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED

        if (needContactPermission || needFingerPrintPermission) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.WRITE_CONTACTS)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                Log.i(TAG, "needs explanation or something")
            } else {
                Log.i(TAG, "requesting permissions")
                requestPermissions(arrayOf(Manifest.permission.WRITE_CONTACTS, Manifest.permission.USE_FINGERPRINT), 2)
            }
        } else {
            proceedToApplication(SPLASH_TIME_OUT)
        }
    }

    fun proceedToApplication(timeOut: Long) {
        Handler().postDelayed({
            // This method will be executed once the timer is over
            val i = Intent(this, MainActivity::class.java)
            startActivity(i)
            // close this activity
            finish()
        }, timeOut)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
         proceedToApplication(SHORT_SPLASH_TIME_OUT)
        }
    }


    companion object {
        val TAG = "SplashScreen"
        private val SPLASH_TIME_OUT: Long = 1000
        private val SHORT_SPLASH_TIME_OUT: Long = 100
    }
}