package com.zeroknowledgeproof.rangeProof

import java.math.BigInteger
import java.math.BigInteger.ONE
import java.math.BigInteger.ZERO
import java.security.SecureRandom

typealias Composite = BigInteger
typealias Base = BigInteger
typealias Commitment = BigInteger
typealias IsSquare = commitVerification

val TWO = BigInteger.valueOf(2)!!

data class setupPrivateResult(
        var m1: BigInteger = ZERO,
        val m2: BigInteger = ZERO,
        val m3: BigInteger = ZERO,
        val r1: BigInteger = ZERO,
        val r2: BigInteger = ZERO,
        val r3: BigInteger = ZERO
)

data class setupResult(
        val c: BigInteger = ZERO,
        val c1: BigInteger = ZERO,
        val c2: BigInteger = ZERO,
        val sameCommitment: commitVerification = commitVerification(),
        val cPrime: BigInteger = ZERO,
        val cDPrime: BigInteger = ZERO,
        val cDPrimeIsSquare: commitVerification = commitVerification(),
        val c1Prime: BigInteger = ZERO,
        val c2Prime: BigInteger = ZERO,
        val c3Prime: BigInteger = ZERO,
        val m3IsSquare: commitVerification = commitVerification(),
        val g: Base = ZERO,
        val h: Base = ZERO,
        val k1: BigInteger = ZERO
)

data class interactiveResult(
        val x: BigInteger = ZERO,
        val y: BigInteger = ZERO,
        val u: BigInteger = ZERO,
        val v: BigInteger = ZERO,
        val challenge: Challenge = Challenge()
)

data class Challenge (
        val s: BigInteger = ZERO,
        val t: BigInteger = ZERO
)

data class commitVerification(
        val g1: Base = ZERO,
        val g2: Base = ZERO,
        val h1: Base = ZERO,
        val h2: Base = ZERO,
        val E: Commitment = ZERO,
        val F: Commitment = ZERO,
        val c: BigInteger = ZERO,
        val D: BigInteger = ZERO,
        val D1: BigInteger = ZERO,
        val D2: BigInteger = ZERO
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
    var res: BigInteger
    do {
        res = BigInteger(upperBound.bitLength(), SecureRandom())
    } while (res > upperBound || res == ZERO || res < lowerBound)
    return res
}

class ZeroKnowledgeException(override var message: String) : Exception() {}