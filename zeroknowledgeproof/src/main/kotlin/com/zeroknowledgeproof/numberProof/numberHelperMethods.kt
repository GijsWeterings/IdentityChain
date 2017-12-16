package com.zeroknowledgeproof.numberProof

import java.math.BigInteger
import java.security.SecureRandom

typealias Challenge = Boolean

fun toBigInt(i: Int) = BigInteger.valueOf(i.toLong())

fun generateRandomInterval(lowerBound: BigInteger, upperBound: BigInteger): BigInteger {
    var res: BigInteger
    do {
        res = BigInteger(upperBound.bitLength(), SecureRandom())
    } while (res > upperBound || res == BigInteger.ZERO || res < lowerBound)
    return res
}

class ZeroKnowledgeException(override var message:String): Exception(){}