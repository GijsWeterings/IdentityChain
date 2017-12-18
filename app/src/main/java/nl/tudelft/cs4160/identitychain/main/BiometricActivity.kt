package nl.tudelft.cs4160.identitychain.main

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_biometric.*
import nl.tudelft.cs4160.identitychain.R
import android.support.v4.content.res.ResourcesCompat
import android.content.res.Resources.Theme
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.View


internal class BiometricActivity : AppCompatActivity(), View.OnClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_biometric)
        val wrapper = ContextThemeWrapper(this, R.style.FingerprintDefaultScene)
        changeTheme(wrapper.theme)

        // TODO: Remove this, only used for demo purposes
        this.fingerprintText.setOnClickListener(this)

    }

    private fun changeTheme(theme: Theme) {
        val drawable = ResourcesCompat.getDrawable(resources, R.drawable.info, theme)
        fingerprintDrawable.setImageDrawable(drawable)
    }

    override fun onClick(v: View?) {
        Log.i(TAG, "Fingerprint sensor read... Verifying")
        val theme = resources.newTheme()

        // TODO: Actually verify the fingerprint sensor, and show either the FingerprintAcceptedScene
        // TODO: (followed by a redirect to the next activity) or the FingerprintDeclinedScene
        val validFingerprint = true

        when (validFingerprint) {
            true -> {
                Log.d(TAG, "Fingerprint accepted")
                theme.applyStyle(R.style.FingerprintAcceptedScene, false)
            }
            else -> {
                Log.d(TAG, "Fingerprint declined")
                theme.applyStyle(R.style.FingerprintDeclinedScene, false)
                fingerprintText.text = getString(R.string.FingerprintDeclinedText)
            }
        }

        changeTheme(theme)
    }
    companion object {

        internal val TAG = "BiometricActivity"
    }

}
