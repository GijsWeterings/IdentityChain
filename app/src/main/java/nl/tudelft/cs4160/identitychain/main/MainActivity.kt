package nl.tudelft.cs4160.identitychain.main


import android.app.KeyguardManager
import android.hardware.fingerprint.FingerprintManager
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import nl.tudelft.cs4160.identitychain.R
import nl.tudelft.cs4160.identitychain.attestation.AttestationFragment
import nl.tudelft.cs4160.identitychain.modals.BiometricAuthenticationFragment
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    val fragments = listOf(PeerConnectFragment(), MainFragment(), AttestationFragment())

    val auth = BiometricAuthenticationFragment()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewPager.adapter = object : FragmentPagerAdapter(this.supportFragmentManager) {
            override fun getItem(position: Int): Fragment = fragments[position]
            override fun getCount(): Int = fragments.size
        }

        viewPager.addOnPageChangeListener(TitleListener(viewPager, this::setTitle, bottomNavigationBar))

        bottomNavigationBar.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.peers -> viewPager.setCurrentItem(0, true)
                R.id.connect -> viewPager.setCurrentItem(1, true)
                R.id.attestationRequest -> viewPager.setCurrentItem(2, true)
            }
            true
        }

        viewPager.setCurrentItem(0, false)
    }
    override fun onStart() {
        super.onStart()
        auth.show(supportFragmentManager, "authenticator")
        auth.setCancelable(false)

        auth.getFingerprintStream(getSystemService(KeyguardManager::class.java), getSystemService(FingerprintManager::class.java))
                .delay(500, TimeUnit.MILLISECONDS)
                .subscribe {
                    if (it) {
                        auth.dismiss()
                    } else {
                        Log.d("MAIN","No valid fingerprint, closing application")
                        finish()
                    }
                }
    }
}

class TitleListener(val viewPager: ViewPager, val setTitle: (String) -> Unit, val bottomNavigationBar: BottomNavigationView) : ViewPager.OnPageChangeListener {
    override fun onPageScrollStateChanged(state: Int) {

    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

    }

    override fun onPageSelected(position: Int) {
        val title = when (viewPager.currentItem) {
            2 -> "Attestation Requests"
            else -> "IdentityChain"
        }
        setTitle(title)
        highlighSelectedItem(position)
    }

    fun highlighSelectedItem(position: Int) {
        val id = when (position) {
            0 -> R.id.peers
            1 -> R.id.connect
            2 -> R.id.attestationRequest
            else -> R.id.peers
        }
        bottomNavigationBar.selectedItemId = id
    }
}
