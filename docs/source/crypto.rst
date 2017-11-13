************
Crypto
************

Trustchain uses the Curve25519 elliptic curve, which is normally used for key agreement, but in trustchain it is used for DSA. The security provider that is used is SpongyCastle, which has a great variety of implemented curves. 

On initial start-up, the app creates a public private key pair which are stored in the local storage of the device. Currently they are stored as a base64 encoded string, and thus unprotected. Currently, the public keys that are used in blocks are X509 encoded certificates.

The class Key contains static functions. These functions can be used to easily load a public/private key, create a signature and verify a signature. 