package com.zeroknowledgeproof

import com.zeroknowledgeproof.rangeProof.pow
import com.zeroknowledgeproof.rangeProof.toBigInt
import java.math.BigInteger
import java.math.BigInteger.ONE
import java.security.SecureRandom

class SecretProof (g1: BigInteger, h1: BigInteger, g2: BigInteger, h2: BigInteger)

class SameSecretProof (givenN: BigInteger) {
    val N = givenN
    val rand = SecureRandom()

    // Security parameters
    val t = 80
    val l = 40
    val b = BigInteger(512, rand)
    val s1 = 40
    val s2 = 552

    val w = getRandomNumber(ONE, toBigInt(2).pow( l + t) * b - ONE)
    val eta1 = getRandomNumber(ONE, toBigInt(2).pow( l + t + s1) * N - ONE)
    val eta2 = getRandomNumber(ONE, toBigInt(2).pow( l + t + s2) * N - ONE)

    val ja = initThis()
    fun initThis () {
        val param = object {
            var g1: BigInteger = ONE
            var g2: BigInteger = ONE
            var h1: BigInteger = ONE
            var h2: BigInteger = ONE

        }

        val W1 = pow(param.g1, w) * pow(param.h1, eta1)
        val W2 = pow(param.g2, w) * pow(param.h2, eta2)

    }

    fun getRandomNumber(lowerBound: BigInteger, upperBound: BigInteger) : BigInteger {
        val rand = SecureRandom()
        val range = upperBound - lowerBound
        var res: BigInteger
        do {
            val bitLength = range.bitLength()
            res = BigInteger(range.bitLength() + 1, rand)
            res += lowerBound
        } while (res > upperBound || res < lowerBound)
        return res
    }
}
