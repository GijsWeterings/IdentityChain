package nl.tudelft.cs4160.identitychain.main


import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentPagerAdapter
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import nl.tudelft.cs4160.identitychain.R
import io.realm.Realm
import nl.tudelft.cs4160.identitychain.attestation.AttestationFragment

class MainActivity : AppCompatActivity() {

    val fragments = listOf(PeerConnectFragment(), MainFragment(), AttestationFragment())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Realm.init(this)
        setContentView(R.layout.activity_main)

        viewPager.adapter = object : FragmentPagerAdapter(this.supportFragmentManager) {
            override fun getItem(position: Int): Fragment = fragments[position]
            override fun getCount(): Int = fragments.size
        }

        bottomNavigationBar.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.peers -> viewPager.setCurrentItem(0, true)
                R.id.connect -> viewPager.setCurrentItem(1, true)
            }
            true
        }

        viewPager.setCurrentItem(0, false)
    }


}
