package nl.tudelft.cs4160.identitychain.modals

import android.app.Application
import android.app.Dialog
import android.arch.lifecycle.*
import android.content.res.Resources.Theme
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.hardware.fingerprint.FingerprintManager
import android.os.Bundle
import android.os.CancellationSignal
import android.support.v4.app.DialogFragment
import android.support.v4.content.res.ResourcesCompat
import android.util.Log
import android.view.*
import android.widget.RelativeLayout
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.disposables.Disposables
import kotlinx.android.synthetic.main.activity_biometric.*
import nl.tudelft.cs4160.identitychain.R
import nl.tudelft.cs4160.identitychain.modals.BiometricAuthenticationFragment.Companion.TAG


class BiometricAuthenticationFragment : DialogFragment() {
    lateinit var biometricAuthenticationViewModel: BiometricAuthenticationViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        biometricAuthenticationViewModel = ViewModelProviders.of(this)[BiometricAuthenticationViewModel::class.java]
        return inflater.inflate(R.layout.activity_biometric, container)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val wrapper = ContextThemeWrapper(this.activity, R.style.FingerprintDefaultScene)
        changeTheme(wrapper.theme)

        biometricAuthenticationViewModel.authenticated.observe(this, Observer<Boolean> { authenticated ->
            if (authenticated == true) {
                this.dismiss()
                //save the auth state in the MainAuthViewmodel so we only show this once.
                ViewModelProviders.of(this.activity)[MainActivityAuthenticated::class.java].authenticated = true
            } else {
                Log.d("MAIN", "No valid fingerprint, closing application")
                this.activity.finish()
            }
        })

        biometricAuthenticationViewModel.uiColors.observe(this, Observer<Boolean> { authenticated ->
            fingerprintTheme(authenticated ?: false)
        })
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // the content
        val root = RelativeLayout(activity)
        root.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        // creating the fullscreen dialog
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(root)
        dialog.window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        return dialog
    }

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
}

class BiometricAuthenticationViewModel(application: Application) : AndroidViewModel(application) {
    private val fingerprintManager: FingerprintManager? = application.getSystemService(FingerprintManager::class.java)
    private val sharedAuthenication = skipIfNoFingerPrintScanner().share()

    val uiColors: LiveData<Boolean> = LiveDataReactiveStreams.fromPublisher(sharedAuthenication)
    val authenticated: LiveData<Boolean> = LiveDataReactiveStreams.fromPublisher(provideTries(sharedAuthenication))


    private fun provideTries(authenticationEvents: Flowable<Boolean>) = Flowable.defer {
        var count = 0
        authenticationEvents.skipWhile {
            val skip = if (count > 4) {
                false
            } else {
                !it
            }
            count++
            skip
        }
    }


    /**
     * If no fingerprint scanner is present just let everybody through.
     * Else return a shared observable, since we need to multicast it.
     * One stream for the UI turning red.
     * A second for the main activity dismissing the fragment
     */
    private fun skipIfNoFingerPrintScanner() = Flowable.defer {
        if (!deviceHasFingerPrintScanner()) {
            Log.i(TAG, "no finger print scanner detected")
            Flowable.just(true)
        } else {
            fingerPrintAuthenticate().toFlowable(BackpressureStrategy.BUFFER)
        }
    }


    private fun fingerPrintAuthenticate(): Observable<Boolean> {
        return Observable.create {
            val authListener = AuthListener(it)
            val cancel = CancellationSignal()

            fingerprintManager?.authenticate(null, cancel, 0, authListener, null)

            it.setDisposable(Disposables.fromAction(cancel::cancel))
        }
    }

    private fun deviceHasFingerPrintScanner() =
            fingerprintManager?.let{ it.isHardwareDetected && it.hasEnrolledFingerprints() } ?: false
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

class MainActivityAuthenticated(var authenticated: Boolean = false) : ViewModel()