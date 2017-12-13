package com.zeroknowledgeproof.rangeProof

import java.math.BigInteger
import java.math.BigInteger.ONE
import java.math.BigInteger.ZERO
import java.security.MessageDigest
import java.security.Security

class RangeProofVerifier (private val N: BigInteger, private val low: Int, private val up: Int) {
    private val TAG = "RangeProofVerifier"

    fun requestChallenge (bound: BigInteger) : Challenge {
        // Step 5: Generate s,t in Zk1 - {0}
        val s = generateRandomInterval(ONE, bound)
        val t = generateRandomInterval(ONE, bound)
        return if (s == t)
            requestChallenge(bound)
        else
            Challenge(s,t)
    }

    fun interactiveVerify(setupRes: setupResult, answer: interactiveResult): Boolean {
        // Step 7: Verifier verifies ALL of these claims.
        // First extract all structs

        val c=setupRes.c
        val c1= setupRes.c1
        val c2 =setupRes.c2
        val sameCommitment = setupRes.sameCommitment
        val cPrime = setupRes.cPrime
        val cDPrime = setupRes.cDPrime
        val cDPrimeIsSquare = setupRes.cDPrimeIsSquare
        val c1Prime = setupRes.c1Prime
        val c2Prime = setupRes.c2Prime
        val c3Prime = setupRes.c3Prime
        val m3IsSquare = setupRes.m3IsSquare
        val g= setupRes.g
        val h =setupRes.h

        val (s,t) = answer.challenge
        val x = answer.x
        val y = answer.y
        val u = answer.u
        val v = answer.v

        // Check that nothing is zero

        return  c != ZERO &&
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

                if(c1.mod(N) != c.times(calculateInverse(g.modPow(toBigInt(low - 1), N), N)).mod(N)) {
                    println("c1 = c/g^(a-1) mod N failed")
                    false

                } else if(c2.mod(N) != g.modPow(toBigInt(up + 1), N).times(calculateInverse(c, N)).mod(N)) {
                    println("c2 = g^(b+1)/c mod N failed")
                    false
                } else if(cDPrime.mod(N) != c1Prime.times(c2Prime).times(c3Prime).mod(N)) {
                    println("c''= c1'c2'c3' mod N failed")
                    false
                } else if(c1Prime.modPow(s, N).times(c2Prime).times(c3Prime).mod(N) != g.modPow(x, N).times(h.modPow(u, N)).mod(N)){
                    println("c1'^s*c2'*c3' = g^x*h^u mod N failed")
                    false
                } else if (c1Prime.times(c2Prime.modPow(t, N)).times(c3Prime).mod(N) != g.modPow(y, N).times(h.modPow(v, N)).mod(N)){
                    println("c1'*c2'^t*c3' = g^y*h^v mod N failed")
                    false
                } else if(x <= ZERO){
                    println("x > 0 failed")
                    false
                } else if(y <= ZERO){
                    println("y > 0 failed")
                    false
                } else {
                    true // Return true;
                }
    }

    private fun verifyIsSquare(m3IsSquare: commitVerification) : Boolean {
        return true;
    }

    private fun verifyTwoCommitments(ver: commitVerification) : Boolean {
        Security.insertProviderAt(org.spongycastle.jce.provider.BouncyCastleProvider(), 1)
        val messageDigest = MessageDigest.getInstance("SHA-512")

        val W1 = ver.g1.modPow(ver.D,N).times(ver.h1.modPow(ver.D1,N)).times(ver.E.modPow(-ver.c, N)).mod(N)
        val W2 = ver.g2.modPow(ver.D,N).times(ver.h2.modPow(ver.D2,N)).times(ver.F.modPow(-ver.c, N)).mod(N)

        if(ver.c != BigInteger(messageDigest.digest(W1.toByteArray() + W2.toByteArray()))){
            println("It can not be verified that the two committments hide the same secret")
            return false
        }
        return true
    }

    fun setupVerify(setupRes: setupResult) : Boolean {
        val c=setupRes.c
        val c1= setupRes.c1
        val c2 =setupRes.c2
        val sameCommitment = setupRes.sameCommitment
        val cPrime = setupRes.cPrime
        val cDPrime = setupRes.cDPrime
        val cDPrimeIsSquare = setupRes.cDPrimeIsSquare
        val c1Prime = setupRes.c1Prime
        val c2Prime = setupRes.c2Prime
        val c3Prime = setupRes.c3Prime
        val m3IsSquare = setupRes.m3IsSquare
        val g= setupRes.g
        val h =setupRes.h

        // First verify that none of the given BigIntegers is zero
        return  c != ZERO &&
                c1 != ZERO &&
                c2 != ZERO &&
                cPrime != ZERO &&
                cDPrime != ZERO &&
                c1Prime != ZERO &&
                c2Prime != ZERO &&
                c3Prime != ZERO &&
                g != ZERO &&
                h != ZERO &&

                // Then verify the three public commitments
                verifyTwoCommitments(sameCommitment) &&
                verifyIsSquare(cDPrimeIsSquare) &&
                verifyIsSquare(m3IsSquare) &&

                // Lastly, we can verify three equations
                c1 == (c * calculateInverse(g.modPow(toBigInt(low - 1), N), N)).mod(N) &&
                c2 == (g.modPow(toBigInt(up + 1), N) * calculateInverse(c, N)).mod(N) &&
                cDPrime.mod(N) != c1Prime.times(c2Prime).times(c3Prime).mod(N)
    }
}