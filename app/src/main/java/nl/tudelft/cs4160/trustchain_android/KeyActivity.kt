package nl.tudelft.cs4160.trustchain_android

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Base64
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_key.*
import nl.tudelft.cs4160.trustchain_android.Util.Key
import java.security.Security

class KeyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_key)
        init()
    }

    private fun init() {
        val kp = Key.ensureKeysExist(applicationContext)
        privateKey.text = Base64.encodeToString(kp.private.encoded, Base64.DEFAULT)


        verifySignature.setOnClickListener {
            val kp = Key.loadKeys(applicationContext)
            val sig = Base64.decode(signedData.text.toString(), Base64.DEFAULT)
            if (Key.verify(kp.public, sampleData, sig)) {
                Toast.makeText(applicationContext, R.string.valid_signature, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(applicationContext, R.string.invalid_signature, Toast.LENGTH_SHORT).show()
            }
        }

        newKey.setOnClickListener {
            val kp = Key.createAndSaveKeys(applicationContext)
            privateKey.text = Base64.encodeToString(kp.private.encoded, Base64.DEFAULT)
        }

        signData.setOnClickListener {
            val kp = Key.loadKeys(applicationContext)
            val sig = Key.sign(kp!!.private, sampleData)
            if (sig == null) {
                println("No sig received")
            }
            signedData.text = Base64.encodeToString(sig, Base64.DEFAULT)
        }

    }

    companion object {
        val sampleData = byteArrayOf(0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30)

        init {
            Security.insertProviderAt(org.spongycastle.jce.provider.BouncyCastleProvider(), 1)
        }
    }


}
