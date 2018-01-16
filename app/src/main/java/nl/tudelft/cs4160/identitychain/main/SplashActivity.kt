package nl.tudelft.cs4160.identitychain.main

import android.support.v7.app.AppCompatActivity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import nl.tudelft.cs4160.identitychain.R
import nl.tudelft.cs4160.identitychain.modals.BiometricAuthenticationFragment


class SplashActivity: AppCompatActivity() {

    // Splash screen timer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler().postDelayed(Runnable {
            // This method will be executed once the timer is over
            val biosensor = Intent(this, MainActivity::class.java)
            startActivity(biosensor)
            // close this activity
            finish()
        }, SPLASH_TIME_OUT)
    }

    companion object {
        private val SPLASH_TIME_OUT: Long = 3000
    }
}