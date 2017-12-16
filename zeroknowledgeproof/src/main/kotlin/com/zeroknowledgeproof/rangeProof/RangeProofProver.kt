package com.zeroknowledgeproof.rangeProof

import java.math.BigInteger
import java.math.BigInteger.ONE
import java.math.BigInteger.ZERO
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.Security

class RangeProofProver (private val m: Int, val a: Int, val b: Int, val N: Composite){
    private val rand = SecureRandom()

    // Security Parameters
    val t = 80
    val l = 40
    val s1 = 40
    val s2 = 552

    /**
     * Generate a range proof that [m] is in the range [[a],[b]]
     * Only the setup part of the rangeproof is performed, still missing is the interactive
     * verification between prover and verifier.
     * @param m Int
     * @param a Int
     * @param b Int
     * @return All generated parameters of the proof, excluding
     */
    fun rangeSetup(that: RangeProofTrustedParty): Pair<SetupPublicResult, SetupPrivateResult> {
        // Check whether we can even generate such a proof
        if (m < a || m > b) {
            throw ZeroKnowledgeException("Cannot generate a false proof without factoring large " +
                    "integers \nPlease come back when you can factor large integers.")
        }
        /////////////////////////////////////////////
        //Init

        // Generate security parameters
        val k2 = BigInteger(160, rand)

        // To generate a generator of order N, we need access to p and q.
        // Kindly ask the Trusted Third Party to do this for us.

        // Generate g, as an element of large order in Zn*
        val g = that.genGenerator()
        //TODO this doesn't actually perform the correct operations yet.
        // Generate h, as an element of large order of the group generated by g.
        val h = that.genGenerator()

        /////////////////////////////////////////////
        /// Step 0: Create initial commitment
        val r = generateRandomInterval(ZERO, k2)
        val c = g.modPow(toBigInt(m), N).times(h.modPow(r, N)).mod(N)

        /// Step 1: calculate c1 and c2
        val c1 = (c * calculateInverse(g.pow(a - 1), N)).mod(N)
        val c2 = (g.pow(b + 1) * calculateInverse(c, N)).mod(N)
        println(">STEP 1 FINISHED")

        // Step 2: Generate rPrime, calculate cPrime, publish it.
        val rPrime = generateRandomInterval(ZERO, k2)
        val cPrime = (c1.modPow(toBigInt(b - m + 1), N) * h.modPow(rPrime, N)).mod(N)

        val proofEqualIntegers = proveTwoCommittedIntegersAreEqual(toBigInt(b - m + 1), -r, rPrime, g, h, c1, h, c2, cPrime)
        println(">STEP 2 FINISHED")

        // Step 3: Choose w from Zk2 - {0}, and r'' from Zk2
        val w = generateRandomInterval(ONE, k2) // Lower bound is 1
        val rDPrime = generateRandomInterval(ZERO, k2)

        // Publicly give a proof that two integers are equal
        val cDPrime = (cPrime.modPow(w * w, N) * h.modPow(rDPrime, N)).mod(N) // The commitment of w in base cPrime
        val cDPrimeIsSquare = proveCommittedNumberIsSquare(w, rDPrime, cPrime, h, cDPrime)
        println(">STEP 3 FINISHED")

        // Step 4: Generate m1, m2, m3
        val sum = w * w * toBigInt((m - a + 1) * (b - m + 1))
        val (m1, m2, m4) = calculateMValues(sum)
        val m3 = m4 * m4

        // Generate r1, r2, r3 such that their sum is equal to w^2 (( b - m + 1)r + r') + r''
        val (r1, r2, r3) = calculateRValues(w * w * (toBigInt(b - m + 1) * r + rPrime) + rDPrime)
        // Then, prove that m3 is a square.
        val c1Prime = (g.modPow(m1, N) * h.modPow(r1, N)).mod(N)
        val c2Prime = (g.modPow(m2, N) * h.modPow(r2, N)).mod(N)
        val c3Prime = (cDPrime * calculateInverse(c1Prime * c2Prime, N)).mod(N)
        val m3IsSquare = proveCommittedNumberIsSquare(m4, r3, g, h, c3Prime)

        println(">STEP 4 FINISHED")

        // Put all results in the result struct, return it.
        val pubRes = SetupPublicResult(g = g, h = h, c = c, c1 = c1, c2 = c2, cPrime = cPrime,
                cDPrime = cDPrime, c1Prime = c1Prime, c2Prime = c2Prime, c3Prime = c3Prime,
                sameCommitment = proofEqualIntegers, cDPrimeIsSquare = cDPrimeIsSquare,
                m3IsSquare = m3IsSquare, k1 = BigInteger(2048, rand))
        // To perform the verification, the verifier needs a bound. This bound is k1, generated by the prover.

        // Save the private parameters, that the verifier should NOT obtain
        val privRes = SetupPrivateResult(m1 = m1, m2 = m2, m3 = m3, r1 = r1, r2 = r2, r3 = r3)
        return Pair(pubRes, privRes)
    }

    fun proveTwoCommittedIntegersAreEqual(committedNum: BigInteger, r1: BigInteger, r2: BigInteger,
                                          g1: BigInteger, h1: BigInteger, g2: BigInteger, h2: BigInteger,
                                          y1: BigInteger, y2: BigInteger): CommittedIntegerProof {

        // Init security parameters
        val b = BigInteger(512, SecureRandom())


        //TODO: Hash function H should output 2t-bit strings.

        // Step 1: Generate random numbers.
        val w = generateRandomInterval(ONE, TWO.pow(l + t) * b - ONE)
        val eta1 = generateRandomInterval(ONE, TWO.pow(l + t + s1) * N - ONE)
        val eta2 = generateRandomInterval(ONE, TWO.pow(l + t + s2) * N - ONE)

        // Generate two commitments
        val W1 = g1.modPow(w, N) * h1.modPow(eta1, N) % N
        val W2 = g2.modPow(w, N).times(h2.modPow(eta2, N)).mod(N)

        // Step 2: Use the hash function.
        Security.insertProviderAt(org.spongycastle.jce.provider.BouncyCastleProvider(), 1)
        val messageDigest = MessageDigest.getInstance("SHA-512")
        // Cast W1 and W2 to byte array's, append them, hash that, and cast it back to a BigInteger
        val c = BigInteger(messageDigest.digest(W1.toByteArray() + W2.toByteArray()))

        // Step 3: Compute verification parameters
        val D = w + c * committedNum
        val D1 = eta1 + c * r1
        val D2 = eta2 + c * r2

        val E =  g1.modPow(committedNum, N).times(h1.modPow(r1, N)).mod(N)
        val F =  g2.modPow(committedNum, N).times(h2.modPow(r2, N)).mod(N)

        if (E != y1 || F != y2) {
            throw ZeroKnowledgeException("The proof that the two commitments are equal" +
                    " could not could be correctly constructed")
        }

        return CommittedIntegerProof(g1, g2, h1, h2, E, F, c, D, D1, D2)
    }

    fun proveCommittedNumberIsSquare(x: BigInteger, r1: BigInteger, g: Base, h: Base, E: Commitment) : IsSquare {
        val n = BigInteger(1024, rand)
        val r2 = generateRandomInterval(-(TWO.pow(s1)) * n + ONE,TWO.pow(s1) * n - ONE )
        val F: Commitment = (g.modPow(x, N).times(h.modPow(r2, N))).mod(N)

        val r3 = r1 - (r2 * x)
        if (E != F.modPow(x, N).times(h.modPow(r3, N)).mod(N))
            throw ZeroKnowledgeException("The prove commited number is square could not be correctly constructed")
        return proveTwoCommittedIntegersAreEqual(committedNum = x, r1 = r2, r2 = r3, g1 = g,
                h1 = h, g2 = F, h2 = h, y1 = F, y2 = E)
    }

    private fun calculateRValues(sum: BigInteger): Triple<BigInteger, BigInteger, BigInteger> {
        val r1 = generateRandomInterval(ONE, sum)
        val r2 = generateRandomInterval(ONE, sum)
        val r3 = sum - r1 - r2
        return Triple(r1, r2, r3)
    }

    private fun calculateMValues(upperBound: BigInteger): Triple<BigInteger, BigInteger, BigInteger> {
        var m1: BigInteger
        val m2: BigInteger
        var m4: BigInteger

        do {
            m1 = generateRandomInterval(ONE, upperBound)
            m4 = generateRandomInterval(ONE, sqrt(upperBound))
        } while (upperBound - m4 * m4 - m1 < ONE)

        m2 = upperBound - m4.pow(2) - m1

        return Triple(m1, m2, m4)
    }
}