package com.zeroknowledgeproof.numberProof

import java.math.BigInteger
import java.security.SecureRandom

object TrustedParty {

    private val debug = false

    private val rand = SecureRandom()
    private val p = BigInteger(1024, 256, rand)
    private val q = BigInteger(1024, 256, rand)

    private val N : BigInteger = p.multiply(q)

    fun proofValue (s: Int = 1800) : Boolean {
        var succeeds = true

        if (debug) println("Hi.")
        val prover = Prover(N, s)
        if (debug) println("We have a prover")
        val verifier = Verifier(N)
        if (debug) println("We have also a verifier")
        // Init done, now run the prover 100 times
        for (t in 1..10) {
            succeeds = succeeds && runProver(prover, verifier)
            if (debug) if (succeeds) println("And ANOTHER success!")
        }

        return succeeds
    }

    private fun runProver(prover: Prover, verifier: Verifier): Boolean {
        val newX : BigInteger = prover.newChallenge()
        if (debug) println("Prover generated new challenge")
        val challenge : Challenge = verifier.requestChallenge()
        if (debug) println("Challenger generated a bit.")
        val y : BigInteger = prover.answerChallenge(newX, challenge) ?: return false
        if (debug) println("Prover answered the challenge: " + y)
        val accepted = verifier.verify(newX, y, prover.getPublicKey(), challenge)
        if (!accepted) {
            if (debug) println("Challenge has NOT been accepted")
            if (debug) println("X: {newX}\nChallenge bit: {challenge}\ny = {y}")
        }

        return accepted
    }

    private fun toBigInt(s: Int) = BigInteger.valueOf(s.toLong())

    fun isCoPrime(s: Int) = isCoPrime(toBigInt(s))

    fun isCoPrime(s: BigInteger) = !(p.mod(s) == BigInteger.ZERO || q.mod(s) == BigInteger.ZERO)


}