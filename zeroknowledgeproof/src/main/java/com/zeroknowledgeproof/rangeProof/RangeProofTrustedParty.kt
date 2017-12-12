package com.zeroknowledgeproof.rangeProof

import java.math.BigInteger
import java.math.BigInteger.ONE
import java.math.BigInteger.ZERO
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.Security

// An implementation based on An Efficient Range Proof Scheme by Kun Peng and Feng Bao

object RangeProofTrustedParty {
    /**
     * Generate a proof that a given number (m) is in a range [a-b]
     */

    val rand = SecureRandom() // Cryptographically secure PRNG

    // Secret prime factors of the large composite N
    private val p = BigInteger(1024, 256, rand) // SECRET
    private val q = BigInteger(1024, 256, rand) // SECRET
    val N: Composite = p.multiply(q) // Can be used many times

    fun runProver(m: Int, a: Int, b: Int) {

    }




    private fun proveCommittedNumberIsSquare() {
//        proveTwoCommittedIntegersAreEqual()
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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

    /**
     * @param N: Large composite, factorization unknown
     * @param g1: Element of large order in Zn*
     */
    private fun proveTwoCommittedIntegersAreEqual(committedNum: BigInteger, r1: BigInteger, r2: BigInteger, N: BigInteger, g1: BigInteger, h1: BigInteger, g2: BigInteger, h2: BigInteger, y1: BigInteger, y2: BigInteger): commitVerification {

        // Init security parameters
        val b = BigInteger(512, rand)
        val t = 80
        val l = 40
        val s1 = 40
        val s2 = 552

        //TODO: Hash function H should output 2t-bit strings.


        // Step 1: Generate random numbers.
        val w = generateRandomInterval(ONE, TWO.pow(l + t) * b - ONE)
        val eta1 = generateRandomInterval(ONE, TWO.pow(l + t + s1) * N - ONE)
        val eta2 = generateRandomInterval(ONE, TWO.pow(l + t + s2) * N - ONE)

        // Generate two commitments
        val W1 = g1.modPow(w, N) * h1.modPow(eta1, N) % N
        val W1ShouldBe = g1.modPow(w, N).times(h1.modPow(eta1, N)).mod(N)
        assert(W1 == W1ShouldBe)

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

        val E = g1.modPow(committedNum, N).times(h1.modPow(r1, N)).mod(N)
        val F = g2.modPow(committedNum, N).times(h2.modPow(r2, N)).mod(N)

        if (E != y1 || F != y2) {
            throw ZeroKnowledgeException("The two committments could not could be correctly constructed")
        }

        return commitVerification(g1, g2, h1, h2, E, F, c, D, D1, D2)
    }


    fun genGenerator(n: Int = 0): BigInteger {
        // Find a generator g, such that g^p mod N and g^q mod N are not equal to 1

        if (n % 100 == 1 && n > 1) println("Finding a generator is more expensive than expected:  " + n)
        val res = BigInteger(1024, rand) // Generate random number

        return if (res.mod(p) == ZERO || res.mod(q) == ZERO) { // Not relatively prime
            genGenerator(n + 1)
        } else if (res.modPow(p, p * q) == ZERO || res.modPow(q, p * q) == ZERO) { // Not a generator, EXPENSIVE CALCULATION
            genGenerator(n + 1)
        } else {
            res // HOORAY WE'VE WON
        }
    }

}