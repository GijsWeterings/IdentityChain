package nl.tudelft.cs4160.identitychain.modals

import android.app.Dialog
import android.app.KeyguardManager
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_biometric.*
import nl.tudelft.cs4160.identitychain.R
import android.support.v4.content.res.ResourcesCompat
import android.content.res.Resources.Theme
import android.graphics.Color
import android.hardware.fingerprint.FingerprintManager
import android.os.CancellationSignal
import android.support.v4.app.DialogFragment
import android.util.Log
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.disposables.Disposables
import android.graphics.drawable.ColorDrawable
import android.view.*
import android.widget.RelativeLayout
import io.reactivex.android.schedulers.AndroidSchedulers




class BiometricAuthenticationFragment : DialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.activity_biometric, container)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val wrapper = ContextThemeWrapper(this.activity, R.style.FingerprintDefaultScene)
        changeTheme(wrapper.theme)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        // the content
        val root = RelativeLayout(activity)
        root.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        // creating the fullscreen dialog
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(root)
        dialog.getWindow().setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        return dialog
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
            fingerprintText.text = "Fingerprint accepted"
        } else {
            Log.d(TAG, "Fingerprint declined")
            theme.applyStyle(R.style.FingerprintDeclinedScene, false)
            fingerprintText.text = getString(R.string.FingerprintDeclinedText)
        }
        changeTheme(theme)
    }


    companion object {

        internal val TAG = "BiometricAuthFragment"
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

    fun getFingerprintStream(keyguardManager: KeyguardManager, fingerprintManager: FingerprintManager): Observable<Boolean> {
        return Observable.defer {
            var count = 0
            this.fingerPrintAuthenticate(keyguardManager, fingerprintManager)
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnNext {
                        fingerprintTheme(it)
                    }
                    .skipWhile {
                        val skip = if (count > 4) {
                            false
                        } else {
                            !it
                        }
                        count++
                        skip
                    }
        }
    }

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

