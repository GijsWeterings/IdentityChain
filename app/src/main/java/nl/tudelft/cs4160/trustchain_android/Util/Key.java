package nl.tudelft.cs4160.trustchain_android.Util;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import org.spongycastle.asn1.x9.X9ECParameters;
import org.spongycastle.crypto.ec.CustomNamedCurves;
import org.spongycastle.jce.ECNamedCurveTable;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.jce.spec.ECParameterSpec;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * Manages key operations.
 */
public class Key {
    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);}

    public final static String PROVIDER = BouncyCastleProvider.PROVIDER_NAME;
    private final static String TAG = "KEY";

    public final static String DEFAULT_PUB_KEY_FILE = "pub.key";
    public final static String DEFAULT_PRIV_KEY_FILE = "priv.key";


    /**
     * Creates a new curve25519 KeyPair.
     * @return KeyPair.
     */
    public static KeyPair createNewKeyPair() {
        return createNewKeyPair("curve25519", "ECDSA", PROVIDER, true);
    }

    /**
     * Creates a new (elliptic curve) KeyPair according to the given arguments.
     * @param curveName The given curnename
     * @param algorithm The used algorithm
     * @param provider The security provider
     * @param custom If this is a custom curve (see BouncyCastle for what custom curves are).
     * @return The generated keypair.
     */
    public static KeyPair createNewKeyPair(String curveName, String algorithm, String provider, boolean custom) {
        ECParameterSpec ecSpec = getParameterSpec(curveName, custom);
        KeyPair keyPair = null;
        try {
            KeyPairGenerator g = KeyPairGenerator.getInstance(algorithm, provider);
            g.initialize(ecSpec, new SecureRandom());
            keyPair = g.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        return keyPair;
    }

    /**
     * Retrieves the parameters of the given elliptic curve.
     * @param curveName The curve name
     * @param custom Custom or not?
     * @return The elliptic curve parameters.
     */
    private static ECParameterSpec getParameterSpec(String curveName, boolean custom) {
        if(custom) {
            X9ECParameters ecP = CustomNamedCurves.getByName(curveName);
            return new ECParameterSpec(ecP.getCurve(), ecP.getG(),
                    ecP.getN(), ecP.getH(), ecP.getSeed());

        }
        return ECNamedCurveTable.getParameterSpec(curveName);
    }


    /**
     * Sign a message using the given private key.
     * @param privateKey The private key
     * @param data The message
     * @return The signature
     */
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

    /**
     * Verify a signature
     * @param publicKey  The public key of the signer.
     * @param msg The message that was signed.
     * @param rawSig The signature.
     * @return True if this a correct signature, false if not.
     */
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

    private static KeyFactory getKeyFactory() {
        try {
            return KeyFactory.getInstance("ECDSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Load a public key from the given file.
     * @param context The context (needed so we can read the file)
     * @param file The file to read.
     * @return The public key.
     */
    public static PublicKey loadPublicKey(Context context, String file) {
        String key = Util.readFile(context, file);
        if(key == null) {
            return null;
        }
        Log.i(TAG, "PUBLIC FROM FILE: " + key);
        return loadPublicKey(key);
    }


    /**
     * Load a raw base64 encoded key.
     * @param key The base64 encoded key.
     * @return Public key
     */
    public static PublicKey loadPublicKey(String key) {
        KeyFactory kf = getKeyFactory();
        if(kf == null) {
            return null;
        }

        byte[] rawKey = Base64.decode(key, Base64.DEFAULT);
        X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(rawKey);

        try {
            return kf.generatePublic(pubKeySpec);
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Load a private key from the given file
     * @param context The context (needed to read the file)
     * @param file The file
     * @return The private key
     */
    public static PrivateKey loadPrivateKey(Context context, String file) {
        String key = Util.readFile(context, file);
        if(key == null) {
            return null;
        }
        Log.i(TAG, "PRIVATE FROM FILE: " + key);
        return loadPrivateKey(key);
    }

    /**
     * Load a private key from a base64 encoded string
     * @param key The base64 encoded key
     * @return The private key
     */
    public static PrivateKey loadPrivateKey(String key) {
        KeyFactory kf = getKeyFactory();
        if(kf == null) {
            return null;
        }

        byte[] rawKey = Base64.decode(key, Base64.DEFAULT);
        PKCS8EncodedKeySpec ks = new PKCS8EncodedKeySpec(rawKey);
        try {
            return kf.generatePrivate(ks);
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Load public and private keys from the standard files.
     * @param context The context (needed to read the files)
     * @return A KeyPair with the private and public key.
     */
    public static KeyPair loadKeys(Context context) {
        PublicKey pubKey = Key.loadPublicKey(context, Key.DEFAULT_PUB_KEY_FILE);
        PrivateKey privateKey = Key.loadPrivateKey(context, Key.DEFAULT_PRIV_KEY_FILE);
        if(pubKey == null || privateKey == null) {
            return null;
        }
        return new KeyPair(pubKey, privateKey);
    }

    /**
     * Write a key to storage
     * @param context Context (needed to write to the file)
     * @param file  The file to write to
     * @param key The key to be written
     * @return True if successful, false if not
     */
    public static boolean saveKey(Context context, String file, java.security.Key key) {
        return Util.writeToFile(context, file, Base64.encodeToString(key.getEncoded(), Base64.DEFAULT));
    }


}
