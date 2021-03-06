package com.zeroknowledgeproof.rangeProof

import java.math.BigInteger
import java.math.BigInteger.ONE
import java.math.BigInteger.ZERO
import java.security.MessageDigest
import java.security.Security

class RangeProofVerifier(private val N: BigInteger, private val a: Int, private val b: Int) {
    fun requestChallenge(bound: BigInteger): Challenge {
        // Step 5: Generate s,t in Zk1 - {0}
        val s = generateRandomInterval(ONE, bound)
        val t = generateRandomInterval(ONE, bound)
        return if (s == t)
            requestChallenge(bound)
        else
            Challenge(s, t)
    }

    fun interactiveVerify(setupRes: SetupPublicResult, answer: InteractivePublicResult): Boolean {
        // Step 7: Verifier verifies ALL of these claims.
        // First extract all structs
        val (c, c1, c2, sameCommitment, cPrime, cDPrime, cDPrimeIsSquare, c1Prime, c2Prime, c3Prime,
                m3IsSquare, g, h, k1) = setupRes

        val (s, t) = answer.challenge
        val (x, y, u, v) = answer

        // Check that nothing is zero

        val isNotZero = setupRes.nonAreZero()
        if (!isNotZero)
            println("Some values have the value zero, none of the parameters of the proof should be zero")

        val verifySameCommitment =
                // First check whether the three commitments in the original setupResult are equal
                verifyTwoCommitments(sameCommitment) &&
                        sameCommitment.g1 == g &&
                        sameCommitment.h1 == h &&
                        sameCommitment.g2 == c1 &&
                        sameCommitment.h2 == h &&
                        sameCommitment.E == c2 &&
                        sameCommitment.F == cPrime

        if (!verifySameCommitment)
            println("The EL proof could not be verified")

        val verifyCDPSquare =
                verifyIsSquare(cDPrimeIsSquare) &&
                        cDPrimeIsSquare.F == cDPrime &&
//                cDPrimeIsSquare.g2 == cDPrimeIsSquare.E && // Already being verified
                        cDPrimeIsSquare.h1 == h &&
                        cDPrimeIsSquare.h2 == h
        if (!verifyCDPSquare)
            println("The first SQR proof could not be verified")

        val verifyM3Square =
                verifyIsSquare(m3IsSquare) &&
                        m3IsSquare.F == c3Prime &&
                        m3IsSquare.g1 == g &&
                        m3IsSquare.h1 == h &&
                        m3IsSquare.h2 == h

        val requirementsSatisfied =
                // If so, verify the seven other requirements. If any of them fails, reject the proof
                when {
                    c1.mod(N) != c.times(calculateInverse(g.modPow(toBigInt(a - 1), N), N)).mod(N) -> {
                        println("c1 = c/g^(a-1) mod N failed")
                        false
                    }
                    c2.mod(N) != g.modPow(toBigInt(b + 1), N).times(calculateInverse(c, N)).mod(N) -> {
                        println("c2 = g^(b+1)/c mod N failed")
                        false
                    }
                    cDPrime.mod(N) != c1Prime.times(c2Prime).times(c3Prime).mod(N) -> {
                        println("c''= c1'c2'c3' mod N failed")
                        false
                    }
                    c1Prime.modPow(s, N).times(c2Prime).times(c3Prime).mod(N) != g.modPow(x, N).times(h.modPow(u, N)).mod(N) -> {
                        println("c1'^s*c2'*c3' = g^x*h^u mod N failed")
                        false
                    }
                    c1Prime.times(c2Prime.modPow(t, N)).times(c3Prime).mod(N) != g.modPow(y, N).times(h.modPow(v, N)).mod(N) -> {
                        println("c1'*c2'^t*c3' = g^y*h^v mod N failed")
                        false
                    }
                    x <= ZERO -> {
                        println("x > 0 failed")
                        false
                    }
                    y <= ZERO -> {
                        println("y > 0 failed")
                        false
                    }
                    else -> true // Return true;
                }
        if (!verifyM3Square)
            println("The second SQR proof could not be verified")

        return isNotZero && verifySameCommitment && verifyCDPSquare && verifyM3Square && requirementsSatisfied
    }

    private fun verifyTwoCommitments(ver: CommittedIntegerProof): Boolean {
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
    private fun verifyIsSquare(ver: IsSquare) : Boolean {
        return ver.g2 == ver.E &&
                verifyTwoCommitments(ver)
    }

    fun setupVerify(setupRes: SetupPublicResult): Boolean {
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
                h != ZERO &&
                k1 != ZERO

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
                c1.mod(N) == (c * calculateInverse(g.modPow(toBigInt(a - 1), N), N)).mod(N) &&
                        c2.mod(N) == (g.modPow(toBigInt(b + 1), N) * calculateInverse(c, N)).mod(N) &&
                        cDPrime.mod(N) == c1Prime.times(c2Prime).times(c3Prime).mod(N)
        if (!eqVerified) {
            println("Could not verify one of the equations")
        }
        return isNonZero && commitmentVerified && eqVerified
    }
}