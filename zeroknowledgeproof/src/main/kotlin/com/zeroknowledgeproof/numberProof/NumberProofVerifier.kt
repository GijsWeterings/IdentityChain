package com.zeroknowledgeproof.numberProof

import java.math.BigInteger
import java.security.SecureRandom

class NumberProofVerifier(givenN: BigInteger) {
    private val debug = false
    private val N = givenN
    private val rand = SecureRandom()

    fun requestChallenge() : Challenge = rand.nextBoolean()

    /**
     *
     */
    fun verify(x: BigInteger, y: BigInteger, publicKey: BigInteger, challenge: Challenge): Boolean {
        if (debug) {
            println("x = " + x.toString(10))
            println("y = " + y.toString(10))
            println("pubkey = " + publicKey.toString(10))
            println("challenge = " + challenge)
            println("N = " + N.toString(10))
        }
        if (y == BigInteger.ZERO)
            throw ZeroKnowledgeException("Y is zero, number proof cannot be verified")

        if ((y * y).mod(N) != (x * publicKey.pow(if (challenge) 1 else 0)).mod(N)) {
            println("Number proof verification failed")
            return false
        }
        return true
    }
}