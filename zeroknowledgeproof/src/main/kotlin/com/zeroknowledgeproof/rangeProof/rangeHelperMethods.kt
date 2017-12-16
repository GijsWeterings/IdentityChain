package com.zeroknowledgeproof.rangeProof

import java.math.BigInteger
import java.math.BigInteger.ONE
import java.math.BigInteger.ZERO
import java.security.SecureRandom

typealias Composite = BigInteger
typealias Base = BigInteger
typealias Commitment = BigInteger
typealias IsSquare = CommittedIntegerProof

val TWO: BigInteger = BigInteger.valueOf(2)

data class SetupPrivateResult(
        val m1: BigInteger,
        val m2: BigInteger,
        val m3: BigInteger,
        val r1: BigInteger,
        val r2: BigInteger,
        val r3: BigInteger
) {
    fun answerUniqueChallenge(challenge: Challenge): InteractivePublicResult {
        // Step 5: Generate s,t in Zk1 - {0} //
        // Already generated and passed to us

        val s = challenge.s
        val t = challenge.t

        // Step 6: NumberProofProver publishes x, y, u, v
        val x = s * m1 + m2 + m3
        val y = m1 + t * m2 + m3

        val u = s * r1 + r2 + r3
        val v = r1 + t * r2 + r3

        return InteractivePublicResult(x = x, y = y, u = u, v = v, challenge = challenge)
    }
}

data class SetupPublicResult(
        val c: BigInteger,
        val c1: BigInteger,
        val c2: BigInteger,
        val sameCommitment: CommittedIntegerProof,
        val cPrime: BigInteger,
        val cDPrime: BigInteger,
        val cDPrimeIsSquare: IsSquare,
        val c1Prime: BigInteger,
        val c2Prime: BigInteger,
        val c3Prime: BigInteger,
        val m3IsSquare: IsSquare,
        val g: Base,
        val h: Base,
        val k1: BigInteger
) {
    fun nonAreZero(): Boolean {
        return c != ZERO &&
                c1 != ZERO &&
                c2 != ZERO &&
                cPrime != ZERO &&
                cDPrime != ZERO &&
                c1Prime != ZERO &&
                c2Prime != ZERO &&
                c3Prime != ZERO &&
                g != ZERO &&
                h != ZERO
    }
}

data class InteractivePublicResult(
        val x: BigInteger,
        val y: BigInteger,
        val u: BigInteger,
        val v: BigInteger,
        val challenge: Challenge
)

data class Challenge(
        val s: BigInteger,
        val t: BigInteger
)

data class CommittedIntegerProof(
        val g1: Base,
        val g2: Base,
        val h1: Base,
        val h2: Base,
        val E: Commitment,
        val F: Commitment,
        val c: BigInteger,
        val D: BigInteger,
        val D1: BigInteger,
        val D2: BigInteger
)

/**
 * Calculate the inverse of [a] in mod [modN]
 * @param a BigInteger
 * @param modN BigInteger
 * @return 1/[a] mod [modN]
 */
fun calculateInverse(a: BigInteger, modN: BigInteger): BigInteger {
    var s = ZERO
    var sp = ONE
    var t = ONE
    var tp = ZERO
    var r = modN
    var rp = a
    var temp: BigInteger
    var q: BigInteger

    while (r != ZERO) {
        q = rp.divide(r)

        temp = r
        r = rp - (q * r)
        rp = temp

        temp = s
        s = sp - (q * s)
        sp = temp

        temp = t
        t = tp - (q * t)
        tp = temp
    }

    return sp
}

// From: https://stackoverflow.com/a/42205084
fun sqrt(n: BigInteger): BigInteger {
    var a = ONE
    var b = n.shiftRight(5).add(BigInteger.valueOf(8))
    while (b >= a) {
        val mid = a.add(b).shiftRight(1)
        if (mid.multiply(mid) > n) {
            b = mid.subtract(ONE)
        } else {
            a = mid.add(ONE)
        }
    }
    return a.subtract(ONE)
}

// Helper function to avoid code clutter
fun toBigInt(i: Int) = BigInteger.valueOf(i.toLong())

/**
 * @param lowerBound Minimum value of the returned BigInteger.
 * @param upperBound Maximum value of the returned BigInteger.
 * @return BigInteger, uniformly distributed between the two bounds, not equal to zero.
 */
fun generateRandomInterval(lowerBound: BigInteger, upperBound: BigInteger): BigInteger {
    if (upperBound <= lowerBound) {
        throw IllegalArgumentException("This is an invalid interval")
    }
    var res: BigInteger
    do {
        res = BigInteger((upperBound - lowerBound).bitLength(), SecureRandom()) + lowerBound
    } while (res > upperBound || res == ZERO || res < lowerBound)
    return res
}

class ZeroKnowledgeException(override var message: String) : Exception() {}