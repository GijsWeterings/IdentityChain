package nl.tudelft.cs4160.trustchain_android;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.security.KeyPair;
import java.security.Security;

import nl.tudelft.cs4160.trustchain_android.Util.Key;

public class KeyActivity extends AppCompatActivity {

    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);}

    private Button buttonNewKey;
    private Button signData;
    private TextView textPrivateKey;
    private TextView signedData;
    private Button verifySignature;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_key);
        init();
    }

    private void init() {
        buttonNewKey = (Button) findViewById(R.id.new_key);
        textPrivateKey = (TextView) findViewById(R.id.private_key);
        signData = (Button) findViewById(R.id.sign_data);
        signedData = (TextView ) findViewById(R.id.signed_data);
        verifySignature = (Button) findViewById(R.id.verify_sig);

        KeyPair kp = Key.loadKeys(getApplicationContext());
        if(kp == null) {
            kp = Key.createNewKeyPair();
            Key.saveKey(getApplicationContext(), Key.DEFAULT_PUB_KEY_FILE, kp.getPublic());
            Key.saveKey(getApplicationContext(), Key.DEFAULT_PRIV_KEY_FILE, kp.getPrivate());
        }
        textPrivateKey.setText(Base64.encodeToString(kp.getPrivate().getEncoded(), Base64.DEFAULT));

        verifySignature.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                KeyPair kp = Key.loadKeys(getApplicationContext());
                byte[] sig = Base64.decode(signedData.getText().toString(), Base64.DEFAULT);
                byte[] data = new byte[] {0x30, 0x30, 0x30, 0x30,0x30, 0x30, 0x30, 0x30};
                if(Key.verify(kp.getPublic(), data, sig)) {
                    Toast.makeText(getApplicationContext(), R.string.valid_signature, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), R.string.invalid_signature, Toast.LENGTH_SHORT).show();
                }
            }
        });

        buttonNewKey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                KeyPair kp = Key.createNewKeyPair();
                Key.saveKey(getApplicationContext(), Key.DEFAULT_PUB_KEY_FILE, kp.getPublic());
                Key.saveKey(getApplicationContext(), Key.DEFAULT_PRIV_KEY_FILE, kp.getPrivate());
                textPrivateKey.setText(Base64.encodeToString(kp.getPrivate().getEncoded(), Base64.DEFAULT));

            }
        });

        signData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                KeyPair kp = Key.loadKeys(getApplicationContext());
                byte[] sig = Key.sign( kp.getPrivate(), new byte[] {0x30, 0x30, 0x30, 0x30,0x30, 0x30, 0x30, 0x30});
                if(sig == null) {
                    System.out.println("No sig received");
                }
                signedData.setText(Base64.encodeToString(sig, Base64.DEFAULT));

            }
        });

    }


}
