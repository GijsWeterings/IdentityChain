package nl.tudelft.cs4160.trustchain_android;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;

import nl.tudelft.cs4160.trustchain_android.Util.Key;

public class KeyActivity extends AppCompatActivity {

    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);}

    public final static String DEFAULT_PUB_KEY_FILE = "pub.key";
    public final static String DEFAULT_PRIV_KEY_FILE = "priv.key";

    private Button buttonNewKey;
    private Button signData;
    private TextView textPrivateKey;
    private TextView textPublicKey;
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
        textPublicKey = (TextView) findViewById(R.id.public_key);
        signData = (Button) findViewById(R.id.sign_data);
        signedData = (TextView ) findViewById(R.id.signed_data);
        verifySignature = (Button) findViewById(R.id.verify_sig);





        KeyPair kp = loadKeys();
        if(kp == null) {
            kp = Key.createNewKeyPair();
            Key.saveKey(getApplicationContext(), DEFAULT_PUB_KEY_FILE, kp.getPublic());
            Key.saveKey(getApplicationContext(), DEFAULT_PRIV_KEY_FILE, kp.getPrivate());
        }
        textPublicKey.setText(Base64.encodeToString(kp.getPublic().getEncoded(), Base64.DEFAULT));
        textPrivateKey.setText(Base64.encodeToString(kp.getPrivate().getEncoded(), Base64.DEFAULT));


        verifySignature.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                KeyPair kp = loadKeys();
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
                Key.saveKey(getApplicationContext(), DEFAULT_PUB_KEY_FILE, kp.getPublic());
                Key.saveKey(getApplicationContext(), DEFAULT_PRIV_KEY_FILE, kp.getPrivate());
                textPublicKey.setText(Base64.encodeToString(kp.getPublic().getEncoded(), Base64.DEFAULT));
                textPrivateKey.setText(Base64.encodeToString(kp.getPrivate().getEncoded(), Base64.DEFAULT));

            }
        });

        signData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                KeyPair kp = loadKeys();
                byte[] sig = Key.sign( kp.getPrivate(), new byte[] {0x30, 0x30, 0x30, 0x30,0x30, 0x30, 0x30, 0x30});
                if(sig == null) {
                    System.out.println("No sig received");
                }
                signedData.setText(Base64.encodeToString(sig, Base64.DEFAULT));

            }
        });

    }


    private KeyPair loadKeys() {
        KeyFactory kf;
        try {
            kf = KeyFactory.getInstance("ECDSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
        PublicKey pubKey = Key.loadPublicKey(getApplicationContext(), DEFAULT_PUB_KEY_FILE, kf);
        PrivateKey privateKey = Key.loadPrivateKey(getApplicationContext(), DEFAULT_PRIV_KEY_FILE, kf);
        if(pubKey == null || privateKey == null) {
            return null;
        }
        KeyPair kp = new KeyPair(pubKey, privateKey);
        return kp;
    }


}
