package com.zeroknowledgeproof.numberProof

import java.math.BigInteger
import java.security.SecureRandom

typealias Challenge = Boolean

class NumberProofProver(givenN: BigInteger, givenS: Int? = null){
    private val debug = false
    private val N = givenN
    private val rand = SecureRandom()
    private val tp = NumberProofTrustedParty;

    private val s : Int = givenS ?: initS()
    private var challenges : MutableMap<BigInteger, BigInteger> = mutableMapOf()

    private fun initS(): Int {
        var toTry: Int;
        do {
            toTry = rand.nextInt()
        } while (!tp.isCoPrime(toTry) || N.subtract(toBigInt(toTry)) > BigInteger.ZERO)
        // We need an integer that is both coprime to N (as in not divisible by p or q) and smaller than N

        return toTry
    }

    private val v : BigInteger = toBigInt(s * s).mod(N)     // Public Key

    fun getPublicKey () : BigInteger = v;

    private fun toBigInt(s: Int) : BigInteger = BigInteger.valueOf(s.toLong())

    fun answerChallenge (x: BigInteger, e: Challenge) : BigInteger? {
        val r = challenges[x] ?: return null
        challenges.remove(x, r)
        if (e) {
            return r.times(toBigInt(s)).mod(N)
        }
        return r
    }

    private fun getR(): BigInteger {
        var possibleR : BigInteger
        do {
            possibleR = BigInteger(1024, rand)
        } while (possibleR.subtract(N) > BigInteger.ONE.negate())
        return possibleR
    }

    fun newChallenge(): BigInteger {
        val r = getR()
        val x = r.multiply(r).mod(N) // r*r mod N
        challenges.put(x, r)
        if (debug) println("r = " + r.toString(10))
        if (debug) println("x = " + x.toString(10))
        return x
    }
}