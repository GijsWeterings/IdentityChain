package com.zeroknowledgeproof.rangeProof

import java.math.BigInteger
import java.math.BigInteger.ONE
import java.math.BigInteger.ZERO
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.Security

class RangeProofProver (private val m: Int, val a: Int, val b: Int, val N: Composite){
    private val rand = SecureRandom()

    /**
     * Generate a range proof that [m] is in the range [[a],[b]]
     * Only the setup part of the rangeproof is performed, still missing is the interactive
     * verification between prover and verifier.
     * @param m Int
     * @param a Int
     * @param b Int
     * @return All generated parameters of the proof, excluding
     */
    fun rangeSetup(): Pair<setupResult, setupPrivateResult> {
        // Check whether we can even generate such a proof
        if (m < a || m > b) {
            throw ZeroKnowledgeException("Cannot generate a false proof without factoring large " +
                    "integers \nPlease come back when you can factor large integers.")
        }

        /////////////////////////////////////////////
        //Init

        // Generate security parameters
//        val k1 = BigInteger(2048, rand)
        val k2 = BigInteger(160, rand)

        // To generate a generator of order N, we need access to p and q.
        // Kindly ask the Trusted Third Party to do this for us.
        val tp = RangeProofTrustedParty

        // Create result data structure
//        val interactiveResult = interactiveProof()
//        interactiveResult.rpr = setupResult
        // Generate g, as an element of large order in Zn*
        val g = tp.genGenerator()
        //TODO this doesn't actually perform the correct operations yet.
        // Generate h, as an element of large order of the group generated by g.
        val h = tp.genGenerator()
        println("We have Generators\n>INIT COMPLETE")


        /////////////////////////////////////////////
        /// Step 0: Create initial commitment
        val r = generateRandomInterval(ZERO, k2)
        val c = g.modPow(toBigInt(m), N).times(h.modPow(r, N)).mod(N)
        println(">STEP 0 COMPLETE")

        /// Step 1: calculate c1 and c2
        val c1 = (c * calculateInverse(g.pow(a - 1), N)) % N
        val c2 = (g.pow(b + 1) * calculateInverse(c, N)) % N
        println(">STEP 1 COMPLETE")

        // Step 2: Generate rPrime, calculate cPrime, publish it.
        val rPrime = generateRandomInterval(ZERO, k2)
        val cPrime = (c1.modPow(toBigInt(b - m + 1), N) * h.modPow(rPrime, N)) % N

        val proofCommitted = proveTwoCommittedIntegersAreEqual(toBigInt(b - m + 1), -r, rPrime, N, g, h, c1, h, c2, cPrime)
        println(">STEP 2 COMPLETE")

        // Step 3: Choose w from Zk2 - {0}, and r'' from Zk2
        val w = generateRandomInterval(ONE, k2) // Lower bound is 1
        val rDPrime = generateRandomInterval(ZERO, k2)
        // Publicly give a proof that two integers are equal

        // This is a problem statement, because it takes impossibly long to perform cPrime^(w*w)
        val cDPrime = (cPrime.modPow(w * w, N) * h.modPow(rDPrime, N)) % N
//        proveCommittedNumberIsSquare() // LIAM, DO STUFF
        println(">STEP 3 COMPLETE")

        // Step 4: Generate m1, m2, m3
        val sum = w * w * toBigInt((m - a + 1) * (b - m + 1))
        val (m1, m2, m4) = calculateMValues(sum)
        val m3 = m4 * m4

        // Generate r1, r2, r3 such that their sum is equal to w^2 (( b - m + 1)r + r') + r''
        val (r1, r2, r3) = calculateRValues(w * w * (toBigInt(b - m + 1) * r + rPrime) + rDPrime)
        // Then, prove that m3 is a square.
        val c1Prime = (g.modPow(m1, N) * h.modPow(r1, N)) % N
        val c2Prime = (g.modPow(m2, N) * h.modPow(r2, N)) % N
        val c3Prime = (cDPrime * calculateInverse(c1Prime * c2Prime, N)) % N

//        proveCommittedNumberIsSquare()// LIAM, DO STUFF
        println(">STEP 4 COMPLETE")


        // Put all results in the result struct, return it.
        val setupRes = setupResult()
        setupRes.g = g
        setupRes.h = h
        setupRes.c = c
        setupRes.c1 = c1
        setupRes.c2 = c2
        setupRes.cPrime = cPrime
        setupRes.cDPrime = cDPrime
        setupRes.c1Prime = c1Prime
        setupRes.c2Prime = c2Prime
        setupRes.c3Prime = c3Prime
        // To perform the verification, the verifier needs a bound. This bound is k1, generated by the prover.
        setupRes.k1 = BigInteger(2048, rand)

        // Save the private parameters, that the verifier should NOT obtain
        val privRes = setupPrivateResult()
        privRes.m1 = m1
        privRes.m2 = m2
        privRes.m3 = m3
        privRes.r1 = r1
        privRes.r2 = r2
        privRes.r3 = r3

        return Pair(setupRes, privRes)
    }

    fun answerUniqueChallenge(privSetup: setupPrivateResult, challenge: Challenge): interactiveResult {
        // Take values from relative structs
        // private values
        val m1 = privSetup.m1
        val m2 = privSetup.m2
        val m3 = privSetup.m3
        val r1 = privSetup.r1
        val r2 = privSetup.r2
        val r3 = privSetup.r3


        // Step 5: Generate s,t in Zk1 - {0} //
        // Already generated and passed to us

        val s = challenge.s
        val t = challenge.t

        // Step 6: NumberProofProver publishes x, y, u, v
        val x = s * m1 + m2 + m3
        val y = m1 + t * m2 + m3

        val u = s * r1 + r2 + r3
        val v = r1 + t * r2 + r3

        val interactiveRes = interactiveResult()
        interactiveRes.challenge = challenge
        interactiveRes.x = x
        interactiveRes.y = y
        interactiveRes.u = u
        interactiveRes.v = v

        return interactiveRes
    }

    fun proveTwoCommittedIntegersAreEqual(committedNum: BigInteger, r1: BigInteger, r2: BigInteger, N: BigInteger, g1: BigInteger, h1: BigInteger, g2: BigInteger, h2: BigInteger, y1: BigInteger, y2: BigInteger): commitVerification {

        // Init security parameters
        val b = BigInteger(512, RangeProofTrustedParty.rand)
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

        val E = (g1.modPow(committedNum, N).times(h1.modPow(r1, N))).mod(N)
        val F = (g2.modPow(committedNum, N).times(h2.modPow(r2, N))).mod(N)

        if (E != y1 || F != y2) {
            println(E.toString(10))
            println(y1.toString(10))
            throw ZeroKnowledgeException("The two committments could not could be correctly constructed")
        }

        return commitVerification(g1, g2, h1, h2, E, F, c, D, D1, D2)
    }

    fun proveCommittedNumberIsSquare() {
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

}