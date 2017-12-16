package com.zeroknowledgeproof.numberProof

import java.math.BigInteger
import java.security.SecureRandom

class NumberProofTrustedParty {

    private val debug = false

    private val rand = SecureRandom()
    private val p = BigInteger(1024, 256, rand)
    private val q = BigInteger(1024, 256, rand)

    private val N : BigInteger = p.multiply(q)

    fun proofValue (s: Int = 1800) : Boolean {
        var succeeds = true

        val prover = NumberProofProver(this, N, s)
        val verifier = NumberProofVerifier(N)
        // Init done, now run the prover 100 times
        for (t in 1..100) {
            succeeds = succeeds && runProver(prover, verifier)
            if (debug) if (succeeds) println("Proof was a success")
        }

        return succeeds
    }

    private fun runProver(numberProofProver: NumberProofProver, numberProofVerifier: NumberProofVerifier): Boolean {
        val newX : BigInteger = numberProofProver.newChallenge()
        if (debug) println("NumberProofProver generated new challenge")
        val challenge : Challenge = numberProofVerifier.requestChallenge()
        if (debug) println("Challenger generated a bit.")
        val y : BigInteger = numberProofProver.answerChallenge(newX, challenge) ?: return false
        if (debug) println("NumberProofProver answered the challenge: " + y)
        val accepted = numberProofVerifier.verify(newX, y, numberProofProver.getPublicKey(), challenge)
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