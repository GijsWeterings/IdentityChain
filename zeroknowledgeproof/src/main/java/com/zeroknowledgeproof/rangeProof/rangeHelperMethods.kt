package com.zeroknowledgeproof.rangeProof

import java.math.BigInteger
import java.math.BigInteger.ONE
import java.math.BigInteger.ZERO

typealias Composite = BigInteger
typealias Base = BigInteger
typealias Commitment = BigInteger

val TWO = BigInteger.valueOf(2)!!

data class setupPrivateResult(
        var m1: BigInteger = ZERO,
        var m2: BigInteger = ZERO,
        var m3: BigInteger = ZERO,
        var r1: BigInteger = ZERO,
        var r2: BigInteger = ZERO,
        var r3: BigInteger = ZERO
)

data class setupResult(
        var c: BigInteger = ZERO,
        var c1: BigInteger = ZERO,
        var c2: BigInteger = ZERO,
        var sameCommitment: commitVerification = commitVerification(),
        var cPrime: BigInteger = ZERO,
        var cDPrime: BigInteger = ZERO,
        var cDPrimeIsSquare: commitVerification = commitVerification(),
        var c1Prime: BigInteger = ZERO,
        var c2Prime: BigInteger = ZERO,
        var c3Prime: BigInteger = ZERO,
        var m3IsSquare: commitVerification = commitVerification(),
        var g: Base = ZERO,
        var h: Base = ZERO,
        var k1: BigInteger = ZERO
)

data class interactiveResult(
        var x: BigInteger = ZERO,
        var y: BigInteger = ZERO,
        var u: BigInteger = ZERO,
        var v: BigInteger = ZERO,
        var challenge: Challenge = Challenge()
)

data class Challenge (
        val s: BigInteger = ZERO,
        val t: BigInteger = ZERO
)

data class commitVerification(
        var g1: Base = ZERO,
        var g2: Base = ZERO,
        var h1: Base = ZERO,
        var h2: Base = ZERO,
        var E: Commitment = ZERO,
        var F: Commitment = ZERO,
        var c: BigInteger = ZERO,
        var D: BigInteger = ZERO,
        var D1: BigInteger = ZERO,
        var D2: BigInteger = ZERO
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
        res = BigInteger(upperBound.bitLength(), RangeProofTrustedParty.rand)
    } while (res > upperBound || res == ZERO || res < lowerBound)
    return res
}

class ZeroKnowledgeException(override var message: String) : Exception() {}