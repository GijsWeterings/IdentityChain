package nl.tudelft.cs4160.identitychain.modals

import android.app.KeyguardManager
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_biometric.*
import nl.tudelft.cs4160.identitychain.R
import android.support.v4.content.res.ResourcesCompat
import android.content.res.Resources.Theme
import android.hardware.fingerprint.FingerprintManager
import android.os.CancellationSignal
import android.support.v4.app.DialogFragment
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.disposables.Disposables


class BiometricAuthenticationFragment : DialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.activity_biometric, container)

        return view
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val wrapper = ContextThemeWrapper(this.activity, R.style.FingerprintDefaultScene)
        changeTheme(wrapper.theme)
    }

    private fun deviceHasFingerPrintScanner(keyguardManager: KeyguardManager) = keyguardManager.isKeyguardSecure

    private fun changeTheme(theme: Theme) {
        val drawable = ResourcesCompat.getDrawable(resources, R.drawable.info, theme)
        fingerprintDrawable.setImageDrawable(drawable)
    }


    fun fingerprintTheme(validFingerprint: Boolean) {
        val theme = resources.newTheme()
        if (validFingerprint) {
            Log.d(TAG, "Fingerprint accepted")
            theme.applyStyle(R.style.FingerprintAcceptedScene, false)
            fingerprintText.text = "Loading application"
        } else {
            Log.d(TAG, "Fingerprint declined")
            theme.applyStyle(R.style.FingerprintDeclinedScene, false)
            fingerprintText.text = getString(R.string.FingerprintDeclinedText)
        }
        changeTheme(theme)
    }


    companion object {

        internal val TAG = "BiometricAuthenticationFragment"
    }

    fun fingerPrintAuthenticate(keyguardManager: KeyguardManager, fingerprintManager: FingerprintManager): Observable<Boolean> {
        return Observable.create {
            if (!deviceHasFingerPrintScanner(keyguardManager)) {
                it.onNext(true)
                it.onComplete()
            } else {
                val authListener = AuthListener(it)
                val cancel = CancellationSignal()

                fingerprintManager.authenticate(null, cancel, 0, authListener, null)

                it.setDisposable(Disposables.fromAction(cancel::cancel))
            }
        }
    }

    fun foo(keyguardManager: KeyguardManager, fingerprintManager: FingerprintManager) = this.fingerPrintAuthenticate(keyguardManager, fingerprintManager).take(5)

    class AuthListener(val em: ObservableEmitter<Boolean>) : FingerprintManager.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: FingerprintManager.AuthenticationResult) {
            em.onNext(true)
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            em.onNext(false)
        }

        override fun onAuthenticationHelp(helpCode: Int, helpString: CharSequence) {
            em.onNext(false)
        }

        override fun onAuthenticationFailed() {
            em.onNext(false)
        }
    }
}

