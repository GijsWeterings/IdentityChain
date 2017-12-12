package com.zeroknowledgeproof.rangeProof

import java.math.BigInteger

data class rangeProofResult (
        var c: BigInteger = BigInteger.ZERO,
        var c1: BigInteger= BigInteger.ZERO,
        var c2: BigInteger= BigInteger.ZERO,
        var sameCommitment: commitVerification = commitVerification(),
        var cPrime: BigInteger = BigInteger.ZERO,
        var cDPrime: BigInteger= BigInteger.ZERO,
        var cDPrimeIsSquare: commitVerification = commitVerification(),
        var c1Prime : BigInteger= BigInteger.ZERO,
        var c2Prime: BigInteger= BigInteger.ZERO,
        var c3Prime: BigInteger= BigInteger.ZERO,
        var m3IsSquare: commitVerification = commitVerification(),
        var g: BigInteger = BigInteger.ZERO,
        var h: BigInteger = BigInteger.ZERO

)

data class interactiveProof (
        var rpr: rangeProofResult = rangeProofResult(),
        var x: BigInteger= BigInteger.ZERO,
        var y: BigInteger= BigInteger.ZERO,
        var u: BigInteger= BigInteger.ZERO,
        var v: BigInteger= BigInteger.ZERO,
        var s: BigInteger = BigInteger.ZERO,
        var t: BigInteger = BigInteger.ZERO
)


data class commitVerification (
        var g1: BigInteger = BigInteger.ZERO,
        var g2: BigInteger = BigInteger.ZERO,
        var h1: BigInteger = BigInteger.ZERO,
        var h2: BigInteger = BigInteger.ZERO,
        var E: BigInteger = BigInteger.ZERO,
        var F: BigInteger = BigInteger.ZERO,
        var c: BigInteger = BigInteger.ZERO,
        var D: BigInteger = BigInteger.ZERO,
        var D1: BigInteger = BigInteger.ZERO,
        var D2: BigInteger = BigInteger.ZERO
)


fun calculateInverse(a: BigInteger, b: BigInteger): BigInteger {
    var s = BigInteger.ZERO
    var sp = BigInteger.ONE
    var t = BigInteger.ONE
    var tp = BigInteger.ZERO
    var r = b
    var rp = a
    var temp: BigInteger
    var q: BigInteger

    while (r != BigInteger.ZERO) {
        q = rp.divide(r)

        temp = r
        r = rp.minus((q.times(r)))
        rp = temp

        temp = s
        s = sp.minus(q.times(s))
        sp = temp

        temp = t
        t = tp.minus((q.times(t)))
        tp = temp
    }

    return sp
}


// From: https://stackoverflow.com/a/42205084
fun sqrt(n: BigInteger): BigInteger {
    var a = BigInteger.ONE
    var b = n.shiftRight(5).add(BigInteger.valueOf(8))
    while (b.compareTo(a) >= 0) {
        val mid = a.add(b).shiftRight(1)
        if (mid.multiply(mid).compareTo(n) > 0) {
            b = mid.subtract(BigInteger.ONE)
        } else {
            a = mid.add(BigInteger.ONE)
        }
    }
    return a.subtract(BigInteger.ONE)
}

fun toBigInt(i: Int) = BigInteger.valueOf(i.toLong())

fun generateRandomInterval(lowerBound: BigInteger, upperBound: BigInteger): BigInteger {
    var res: BigInteger
    do {
        res = BigInteger(upperBound.bitLength(), RangeProofTrustedParty.rand)
    } while (res > upperBound || res == BigInteger.ZERO || res < lowerBound)
    return res
}

class ZeroKnowledgeException(override var message:String): Exception(){}