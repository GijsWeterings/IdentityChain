package nl.tudelft.cs4160.trustchain_android;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import org.spongycastle.jce.interfaces.ECPrivateKey;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

/**
 * Created by rico on 14-9-17.
 */

public class Key {
    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);}

    private final static String PROVIDER = "SC";

    private KeyPair keyPair;

    public final static String PREF_PUBLIC_KEY = "PUB_KEY";
    public final static String PREF_PRIVATE_KEY = "PRIV_KEY";

    public Key() throws NoSuchProviderException, NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        createNewKey();
    }

    public Key(KeyPair kp) {
        keyPair = kp;
    }

    private void createNewKey() throws NoSuchProviderException, NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        ECGenParameterSpec ecParamSpec = new ECGenParameterSpec("secp256k1");
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("ECDSA", PROVIDER);
        kpg.initialize(ecParamSpec);
        keyPair=kpg.generateKeyPair();

    }

    public PublicKey getPublic() {
        return keyPair.getPublic();
    }

    public PrivateKey getPrivate() {
        return keyPair.getPrivate();
    }




    public byte[] sign(byte[] data) {
        try {
            Signature sig = Signature.getInstance("SHA256withECDSA", PROVIDER);
            sig.initSign(getPrivate());
            sig.update(data);
            return sig.sign();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (SignatureException e) {
            e.printStackTrace();
        }
        return null;
    }

}
