package nl.tudelft.cs4160.trustchain_android.Util;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * Created by rico on 14-9-17.
 */

public class Key {
    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);}

    public final static String PROVIDER = BouncyCastleProvider.PROVIDER_NAME;
    private final static String TAG = "KEY";




    public static KeyPair createNewKeyPair() {
        return createNewKeyPair("secp256k1", "ECDSA", PROVIDER);
    }

    public static KeyPair createNewKeyPair(String stdName, String algorithm, String provider) {
        ECGenParameterSpec ecParamSpec = new ECGenParameterSpec(stdName);
        KeyPairGenerator kpg;
        try {
            kpg = KeyPairGenerator.getInstance(algorithm, provider);
            kpg.initialize(ecParamSpec);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
            return null;
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
            return null;
        }
        return kpg.generateKeyPair();
    }




    public static byte[] sign(PrivateKey privateKey, byte[] data) {
        try {
            Signature sig = Signature.getInstance("SHA256withECDSA", PROVIDER);
            sig.initSign(privateKey);
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



    public static boolean verify(PublicKey publicKey, byte[] msg, byte[] rawSig) {
        try {
            Signature sig = Signature.getInstance("SHA256withECDSA", PROVIDER);
            sig.initVerify(publicKey);
            sig.update(msg);
            return sig.verify(rawSig);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (SignatureException e) {
            e.printStackTrace();
        }
        return false;
    }


    public static PublicKey loadPublicKey(Context context, String file, KeyFactory kf) {
        String key = Util.readFile(context, file);
        if(key == null) {
            return null;
        }
        Log.d(TAG, "PUBLIC FROM FILE" + key);
        byte[] rawKey = Base64.decode(key, Base64.DEFAULT);
        X509EncodedKeySpec ks = new X509EncodedKeySpec(rawKey);
        try {
            return kf.generatePublic(ks);
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static PrivateKey loadPrivateKey(Context context, String file, KeyFactory kf) {
        String key = Util.readFile(context, file);
        if(key == null) {
            return null;
        }
        Log.d(TAG, "PRIVATE FROM FILE" + key);
        byte[] rawKey = Base64.decode(key, Base64.DEFAULT);
        PKCS8EncodedKeySpec ks = new PKCS8EncodedKeySpec(rawKey);
        try {
            return kf.generatePrivate(ks);
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean saveKey(Context context, String file, java.security.Key key) {
        return Util.writeToFile(context, file, Base64.encodeToString(key.getEncoded(), Base64.DEFAULT));
    }


}
