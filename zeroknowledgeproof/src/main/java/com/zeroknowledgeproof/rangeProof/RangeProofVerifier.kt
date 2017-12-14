package com.zeroknowledgeproof.rangeProof

import java.math.BigInteger
import java.math.BigInteger.ONE
import java.math.BigInteger.ZERO
import java.security.MessageDigest
import java.security.Security

class RangeProofVerifier(private val N: BigInteger, private val low: Int, private val up: Int) {
    private val TAG = "RangeProofVerifier"

    fun requestChallenge(bound: BigInteger): Challenge {
        // Step 5: Generate s,t in Zk1 - {0}
        val s = generateRandomInterval(ONE, bound)
        val t = generateRandomInterval(ONE, bound)
        return if (s == t)
            requestChallenge(bound)
        else
            Challenge(s, t)
    }

    fun interactiveVerify(setupRes: setupResult, answer: interactiveResult): Boolean {
        // Step 7: Verifier verifies ALL of these claims.
        // First extract all structs
        val (c, c1, c2, sameCommitment, cPrime, cDPrime, cDPrimeIsSquare, c1Prime, c2Prime, c3Prime,
                m3IsSquare, g, h, k1) = setupRes

        val (s, t) = answer.challenge
        val (x, y, u, v) = answer

        // Check that nothing is zero

        return c != ZERO &&
                c1 != ZERO &&
                c2 != ZERO &&
                cPrime != ZERO &&
                cDPrime != ZERO &&
                c1Prime != ZERO &&
                c2Prime != ZERO &&
                c3Prime != ZERO &&
                g != ZERO &&
                h != ZERO &&

                // First check whether the three commitments in the original setupResult are equal
                verifyTwoCommitments(sameCommitment) &&
                verifyIsSquare(cDPrimeIsSquare) &&
                verifyIsSquare(m3IsSquare) &&

                // If so, verify the seven other requirements. If any of them fails, reject the proof

                if (c1.mod(N) != c.times(calculateInverse(g.modPow(toBigInt(low - 1), N), N)).mod(N)) {
                    println("c1 = c/g^(a-1) mod N failed")
                    false

                } else if (c2.mod(N) != g.modPow(toBigInt(up + 1), N).times(calculateInverse(c, N)).mod(N)) {
                    println("c2 = g^(b+1)/c mod N failed")
                    false
                } else if (cDPrime.mod(N) != c1Prime.times(c2Prime).times(c3Prime).mod(N)) {
                    println("c''= c1'c2'c3' mod N failed")
                    false
                } else if (c1Prime.modPow(s, N).times(c2Prime).times(c3Prime).mod(N) != g.modPow(x, N).times(h.modPow(u, N)).mod(N)) {
                    println("c1'^s*c2'*c3' = g^x*h^u mod N failed")
                    false
                } else if (c1Prime.times(c2Prime.modPow(t, N)).times(c3Prime).mod(N) != g.modPow(y, N).times(h.modPow(v, N)).mod(N)) {
                    println("c1'*c2'^t*c3' = g^y*h^v mod N failed")
                    false
                } else if (x <= ZERO) {
                    println("x > 0 failed")
                    false
                } else if (y <= ZERO) {
                    println("y > 0 failed")
                    false
                } else {
                    true // Return true;
                }
    }

    private fun verifyTwoCommitments(ver: commitVerification): Boolean {
        Security.insertProviderAt(org.spongycastle.jce.provider.BouncyCastleProvider(), 1)
        val messageDigest = MessageDigest.getInstance("SHA-512")

        val W1 = ver.g1.modPow(ver.D, N).times(ver.h1.modPow(ver.D1, N)).times(ver.E.modPow(-ver.c, N)).mod(N)
        val W2 = ver.g2.modPow(ver.D, N).times(ver.h2.modPow(ver.D2, N)).times(ver.F.modPow(-ver.c, N)).mod(N)

        if (ver.c != BigInteger(messageDigest.digest(W1.toByteArray() + W2.toByteArray()))) {
            println("It can not be verified that the two committments hide the same secret")
            return false
        }
        return true
    }

    /**
     * The proof that the committed number is a square only holds if the base of the second number
     * is actually the outcome of the first number.
     */
    private fun verifyIsSquare(ver: commitVerification) : Boolean {
        return ver.g2 == ver.E &&
                verifyTwoCommitments(ver)
    }

    fun setupVerify(setupRes: setupResult): Boolean {
        val (c, c1, c2, sameCommitment, cPrime, cDPrime, cDPrimeIsSquare, c1Prime, c2Prime, c3Prime,
                m3IsSquare, g, h, k1) = setupRes

        // First verify that none of the given BigIntegers is zero
        val isNonZero = c != ZERO &&
                c1 != ZERO &&
                c2 != ZERO &&
                cPrime != ZERO &&
                cDPrime != ZERO &&
                c1Prime != ZERO &&
                c2Prime != ZERO &&
                c3Prime != ZERO &&
                g != ZERO &&
                h != ZERO

        if (!isNonZero) {
            println("Some values are zero. That can't happen.")
        }

        // Then verify the three public commitments
        val commitmentVerified =
                verifyTwoCommitments(sameCommitment) &&
                        verifyTwoCommitments(cDPrimeIsSquare) &&
                        verifyTwoCommitments(m3IsSquare)
        if (!commitmentVerified)
            println("One of the public commitments could not be verified")

        // Lastly, we can verify three equations
        val eqVerified =
                c1.mod(N) == (c * calculateInverse(g.modPow(toBigInt(low - 1), N), N)).mod(N) &&
                        c2.mod(N) == (g.modPow(toBigInt(up + 1), N) * calculateInverse(c, N)).mod(N) &&
                        cDPrime.mod(N) == c1Prime.times(c2Prime).times(c3Prime).mod(N)
        if (!eqVerified) {
            println("Could not verify one of the equations")
        }
        return isNonZero && commitmentVerified && eqVerified
    }
}