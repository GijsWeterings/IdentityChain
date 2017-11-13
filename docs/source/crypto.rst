************
Crypto
************

Trustchain uses the Curve25519 elliptic curve, which is normally used for key agreement, but in trustchain it is used for DSA. The security provider that is used is SpongyCastle, which has a great variety of implemented curves. 

The class Key is a helper class, which can be used to perform crypographic functions. Most of the functions in this class are static functions, so that they can be called from every place. These functions can be used to easily load a public/private key, create a signature, create a new key pair, and verify a signature. 

On initial start-up, the app creates a public private key pair which are then stored in the local storage of the device. 