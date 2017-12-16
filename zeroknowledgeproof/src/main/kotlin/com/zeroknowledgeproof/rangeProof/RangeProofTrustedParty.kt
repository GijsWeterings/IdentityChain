package com.zeroknowledgeproof.rangeProof

import java.math.BigInteger
import java.math.BigInteger.ZERO
import java.security.SecureRandom

// An implementation based on An Efficient Range Proof Scheme by Kun Peng and Feng Bao

class RangeProofTrustedParty {
    /**
     * Generate a proof that a given number (m) is in a range [a-b]
     */

    private val rand = SecureRandom() // Cryptographically secure PRNG

    // Secret prime factors of the large composite N
    private val p = BigInteger(1024, 256, rand) // SECRET
    private val q = BigInteger(1024, 256, rand) // SECRET
    val N: Composite = p.multiply(q) // Can be used many times

    fun generateProof(m: Int, a: Int, b: Int): Pair<SetupPublicResult, SetupPrivateResult> {
        val p = RangeProofProver(m = m, a = a, b = b, N = N)
        return p.rangeSetup(this) // Step 1-4
    }

    fun runInteractiveProver(m: Int, a: Int, b: Int): Boolean {
        val (setupPublic, setupPrivate) = generateProof(m, a, b)
        val v = RangeProofVerifier(N = N, low = a, up = b)
        // The verifier should interactiveVerify everything possible at this point
        var success = v.setupVerify(setupPublic)
        if (!success) {
            println("Could not verify setup parameters")
            return false
        }
        println("Successfully generated setup parameters")
        for (i in 1..100) {
            println("Generating new interactive challenge")
            // First generate a new challenge
            val challenge = v.requestChallenge(setupPublic.k1) // Step 5
            val interactiveAnswer = setupPrivate.answerUniqueChallenge(challenge) // Step 6
            success = v.interactiveVerify(setupPublic, interactiveAnswer)

            if (!success) {
                println("Could not verify interactive prover " + i)
                return false
            }
        }
        println("Great success in constructing a rangeproof")
        return true
    }

    /**
     * @param N: Large composite, factorization unknown
     * @param g1: Element of large order in Zn*
     */

    fun genGenerator(): Base {
        // Find a res g, such that g^p mod N and g^q mod N are not equal to 1
        var res: Base
        do {
            res = BigInteger(1024, rand)
        } while (res % p == ZERO ||
                res % q == ZERO ||
                res.modPow(p, p * q) == ZERO ||
                res.modPow(q, p * q) == ZERO)
        return res
    }
}