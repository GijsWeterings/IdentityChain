package nl.tudelft.cs4160.trustchain_android;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class KeyActivity extends AppCompatActivity {

    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);}

    private Button buttonNewKey;
    private Button signData;
    private TextView textPrivateKey;
    private TextView textPublicKey;
    private TextView signedData;

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


        KeyPair kp = loadKeys();
        if(kp != null) {
            textPublicKey.setText(Base64.encodeToString(kp.getPublic().getEncoded(), Base64.DEFAULT));
            textPrivateKey.setText(Base64.encodeToString(kp.getPrivate().getEncoded(), Base64.DEFAULT));
        }

        buttonNewKey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    Key k = new Key();
                    storeKeys(k);

                } catch (NoSuchProviderException e) {
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (InvalidAlgorithmParameterException e) {
                    e.printStackTrace();
                }

                KeyPair kp = loadKeys();
                textPublicKey.setText(Base64.encodeToString(kp.getPublic().getEncoded(), Base64.DEFAULT));
                textPrivateKey.setText(Base64.encodeToString(kp.getPrivate().getEncoded(), Base64.DEFAULT));

            }
        });

        signData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                KeyPair kp = loadKeys();
                Key k =new Key(kp);
                byte[] sig =k.sign(new byte[] {0x30, 0x30, 0x30, 0x30,0x30, 0x30, 0x30, 0x30});
                if(sig == null) {
                    System.out.println("No sig received");
                } else {
                    for (int i = 0; i < sig.length; i++) {
                        System.out.println(String.valueOf(sig[i]));
                    }
                }
                signedData.setText(Base64.encodeToString(sig, Base64.DEFAULT));

            }
        });

    }


    private void storeKeys(Key key) {
        storeKey(key.getPrivate(), true);
        storeKey(key.getPublic(), false);
    }

    private void storeKey(java.security.Key key, boolean privateKey) {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        String keyString = Base64.encodeToString(key.getEncoded(), Base64.DEFAULT);
        if(privateKey) {
            editor.putString(Key.PREF_PRIVATE_KEY, keyString);
        } else {
            editor.putString(Key.PREF_PUBLIC_KEY, keyString);
        }
        editor.commit();
    }

    private KeyPair loadKeys() {
        KeyFactory kf;
        try {
            kf = KeyFactory.getInstance("ECDSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
        PublicKey pubKey = loadPublicKey(loadX509Key(Key.PREF_PUBLIC_KEY), kf);
        PrivateKey privateKey = loadPrivateKey(loadPKCS8Key(Key.PREF_PRIVATE_KEY), kf);
        if(pubKey == null || privateKey == null) {
            return null;
        }
        KeyPair kp = new KeyPair(pubKey, privateKey);
        return kp;
    }

    public X509EncodedKeySpec loadX509Key(String keyPref) {
        SharedPreferences sharedPrefs = getPreferences(Context.MODE_PRIVATE);
        String base64Key = sharedPrefs.getString(keyPref, "");
        if (base64Key.isEmpty()) {
            return null;
        }
        System.out.println(base64Key);
        byte[] array = Base64.decode(base64Key, Base64.DEFAULT);
        X509EncodedKeySpec ks = new X509EncodedKeySpec(array);
        return ks;
    }

    public PKCS8EncodedKeySpec loadPKCS8Key(String keyPref) {
        SharedPreferences sharedPrefs = getPreferences(Context.MODE_PRIVATE);
        String base64Key = sharedPrefs.getString(keyPref, "");
        if (base64Key.isEmpty()) {
            return null;
        }
        System.out.println(base64Key);
        byte[] array = Base64.decode(base64Key, Base64.DEFAULT);
        PKCS8EncodedKeySpec ks = new PKCS8EncodedKeySpec(array);
        return ks;
    }


    public PublicKey loadPublicKey(X509EncodedKeySpec ks, KeyFactory kf) {
        ECPublicKey remotePublicKey;
        try {
            remotePublicKey = (ECPublicKey)kf.generatePublic(ks);
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
            return null;
        } catch (ClassCastException e) {
            e.printStackTrace();
            return null;
        }
        return remotePublicKey;
    }

    public PrivateKey loadPrivateKey(PKCS8EncodedKeySpec ks, KeyFactory kf) {
        ECPrivateKey privKey;
        try {
            privKey = (ECPrivateKey)kf.generatePrivate(ks);
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
            return null;
        } catch (ClassCastException e) {
            e.printStackTrace();
            return null;
        }
        return privKey;

    }




}
