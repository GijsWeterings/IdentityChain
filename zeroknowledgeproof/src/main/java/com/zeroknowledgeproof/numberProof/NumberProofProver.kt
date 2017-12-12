package com.zeroknowledgeproof.numberProof

import java.math.BigInteger
import java.math.BigInteger.ONE
import java.math.BigInteger.ZERO
import java.security.SecureRandom

class NumberProofProver(givenN: BigInteger, givenS: Int? = null) {

    private val debug = false
    private val N = givenN
    private val rand = SecureRandom()
    private val tp = NumberProofTrustedParty;

    private val s: Int = givenS ?: initS()
    private var challenges: MutableMap<BigInteger, BigInteger> = mutableMapOf()

    private fun initS(): Int {
        var toTry: Int;
        do {
            toTry = rand.nextInt()
        } while (!tp.isCoPrime(toTry) || N - toBigInt(toTry) > BigInteger.ZERO)
        // We need an integer that is both coprime to N (as in not divisible by p or q) and smaller than N

        return toTry
    }

    private val v: BigInteger = toBigInt(s * s).mod(N)     // Public Key

    fun getPublicKey(): BigInteger = v;

    fun answerChallenge(x: BigInteger, e: Challenge): BigInteger? {
        val r = challenges[x] ?: return null
        challenges.remove(x, r)
        if (e) {
            return r.times(toBigInt(s)).mod(N)
        }
        return r
    }

    fun newChallenge(): BigInteger {
        val r = generateRandomInterval(ZERO, N-ONE)
        val x = r.multiply(r).mod(N) // r*r mod N
        challenges.put(x, r)
        if (debug) println("r = " + r.toString(10))
        if (debug) println("x = " + x.toString(10))
        return x
    }
}

