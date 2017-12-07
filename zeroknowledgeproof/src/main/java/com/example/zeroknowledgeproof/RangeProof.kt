package com.example.zeroknowledgeproof

import java.math.BigInteger
import java.math.BigInteger.ONE
import java.security.SecureRandom
import kotlin.math.sqrt
import com.example.zeroknowledgeproof.SecretOrderGroupGenerator
import com.example.zeroknowledgeproof.SecretOrderGroupGenerator.Companion.findGenerators
import com.example.zeroknowledgeproof.SecretOrderGroupGenerator.Companion.generateSafePrimes


// An implementation based on An Efficient Range Proof Scheme by Kun Peng and Feng Bao



class RangeProof () {
    /**
     * Generate a proof that a given number is in a range
     */
    val g = BigInteger("5866666666666666666666666666666666666666666666666666666666666666", 16)

    fun Prover (m: Int, a: Int, b: Int) : String {
        // Prove that boundedNumber is between lower and upper bound
        if ((m - a + 1)*(b - m + 1) <= 0) {
            throw IllegalArgumentException("Cannot generate proof, because this is just NOT TRUE")
        }

        val rand = SecureRandom(); // SEKURE random

        val w = rand.nextInt(10000)

        val theProof = w * w * (m - a + 1) * (b - m + 1)

        val m4 = rand.nextInt(sqrt((Integer.MAX_VALUE).toDouble()).toInt())
        val m3 = m4*m4;
        val m2 = rand.nextInt();
        val m1 = theProof - m3 - m2      // Now m1 + m2 + m3 = w^2 * (m-a+1) * (b-m+1)

        var P: BigInteger
        var Q: BigInteger
        var p: BigInteger
        var q: BigInteger

        var attempts = 0;
        do {
            P = BigInteger.probablePrime(2048, rand)
            attempts++
            if (attempts % 100 == 0) {
                println("Calculating P and p, now at " + attempts + " attempts.")
            }
        } while (!P.min(ONE).divide(BigInteger.valueOf(2)).isProbablePrime(128) )

        do {
            Q = BigInteger.probablePrime(2048, rand)
            attempts++
            if (attempts % 100 == 0) {
                println("Calculating Q and q now at " + attempts + " attempts.")
            }
        } while (!Q.min(ONE).divide(BigInteger.valueOf(2)).isProbablePrime(128) || P.equals(Q))

        val N = P.multiply(Q)

        val k1 = BigInteger(1024, rand)
        val k2 = BigInteger(160, rand)

        val (g,h) = findGenerators(generateSafePrimes(2048, 128))

        val r = BigInteger(N.bitLength(), rand)

        val c = g.modPow(toBigInt(m), N).times(h.modPow(r, N)).mod(N)

        val c1 = c.divide(g.modPow(toBigInt(a-1), N)).mod(N) // c1 = c/(g^a-1) modN
        //TODO Maybe this should not be Integer division, but cyclic group division (groot crypto fun)
        val c2 = g.modPow(toBigInt(b+1), N).divide(c).mod(N)
        val rPrime = BigInteger(N.bitLength(), rand)

        val cPrime = c1.modPow(toBigInt(b - m + 1), N).times(h.modPow(rPrime, N)).mod(N)
        val rDPrime = BigInteger(N.bitLength(), rand)

        val cDPrime = cPrime.modPow(toBigInt(w*w), N).times(h.modPow(rDPrime, N)).mod(N)

        val publicProof = cPrime.toString(10) + "," + h.toString(10)
        print("The Public Proof is " + publicProof)

        // Prove that the hidden number is a square with base (c', h)

        return "aapje"
    }

    private fun toBigInt(i: Int) : BigInteger = BigInteger.valueOf(i.toLong())

}