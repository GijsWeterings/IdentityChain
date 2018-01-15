package nl.tudelft.cs4160.identitychain.modals

import android.app.KeyguardManager
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_biometric.*
import nl.tudelft.cs4160.identitychain.R
import android.support.v4.content.res.ResourcesCompat
import android.content.res.Resources.Theme
import android.hardware.fingerprint.FingerprintManager
import android.os.CancellationSignal
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.View
import nl.tudelft.cs4160.identitychain.main.MainActivity


internal class BiometricActivity : AppCompatActivity(), View.OnClickListener {

    private var cancellationSignal: CancellationSignal? = null
    lateinit var keyguardManager : KeyguardManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_biometric)
        val wrapper = ContextThemeWrapper(this, R.style.FingerprintDefaultScene)
        changeTheme(wrapper.theme)
        keyguardManager = getSystemService(KeyguardManager::class.java)

        //If no fingerprint scanner is found on the device skip it for now
        if(!keyguardManager.isKeyguardSecure){
            loadApplication()
        }
    }

    private fun changeTheme(theme: Theme) {
        val drawable = ResourcesCompat.getDrawable(resources, R.drawable.info, theme)
        fingerprintDrawable.setImageDrawable(drawable)
    }

    fun startListening() {
        val fingerprintMgr = getSystemService(FingerprintManager::class.java)
        fingerprintMgr.authenticate(null, cancellationSignal, 0, fingerCallback, null)

    }


    fun stopListening() {
        cancellationSignal?.also {
            it.cancel()
        }
        cancellationSignal = null

    }


    override fun onClick(v: View?) {
        Log.i(TAG, "Fingerprint sensor read... Verifying")

        // TODO: Actually verify the fingerprint sensor, and show either the FingerprintAcceptedScene
        // TODO: (followed by a redirect to the next activity) or the FingerprintDeclinedScene
        val validFingerprint = true
        fingerprintTheme(validFingerprint)

    }

    fun fingerprintTheme(validFingerprint: Boolean) {
        val theme = resources.newTheme()
        when (validFingerprint) {
            true -> {
                Log.d(TAG, "Fingerprint accepted")
                theme.applyStyle(R.style.FingerprintAcceptedScene, false)
                fingerprintText.text = "Loading application"
                loadApplication()
            }
            else -> {
                Log.d(TAG, "Fingerprint declined")
                theme.applyStyle(R.style.FingerprintDeclinedScene, false)
                fingerprintText.text = getString(R.string.FingerprintDeclinedText)
            }
        }

        changeTheme(theme)
    }

    fun loadApplication() {
        val i = Intent(this, MainActivity::class.java)
        startActivity(i)
        //Finish this activity
        finish()
    }

    companion object {

        internal val TAG = "BiometricActivity"
    }

    private val fingerCallback = object : FingerprintManager.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: FingerprintManager.AuthenticationResult) {
            fingerprintTheme(true)
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            fingerprintTheme(false)
        }

        override fun onAuthenticationHelp(helpCode: Int, helpString: CharSequence) {
            fingerprintTheme(false)
        }

        override fun onAuthenticationFailed() {
            fingerprintTheme(false)
        }
    }

    override fun onResume() {
        super.onResume()
        if (keyguardManager.isKeyguardSecure) {
            this.startListening()
        }
    }

    override fun onPause() {
        super.onPause()
        if (keyguardManager.isKeyguardSecure) {

            this.stopListening()
        }
    }
}

