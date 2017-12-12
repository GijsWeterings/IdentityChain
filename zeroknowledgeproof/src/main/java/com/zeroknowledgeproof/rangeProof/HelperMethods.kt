package com.zeroknowledgeproof.rangeProof

import java.math.BigInteger


class HelperMethods {

}


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