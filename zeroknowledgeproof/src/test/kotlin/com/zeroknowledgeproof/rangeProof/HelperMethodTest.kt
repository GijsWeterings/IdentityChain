package com.zeroknowledgeproof.rangeProof

import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger
import java.security.SecureRandom
import java.util.*

class HelperMethodTest{
    @Test
    fun testToBigInt() {
        val i = 124_485_684
        assertTrue(BigInteger.valueOf(i.toLong()) == toBigInt(i))
    }

    @Test
    fun testGenerateInInterval() {
        val lower = BigInteger("1020304050607", 10)
        val upper = BigInteger("1020304050607080910", 10)
        val r = generateRandomInterval(lower, upper)
        assertTrue(r > lower && r < upper)
        assertTrue(r != BigInteger.ZERO)
    }

    @Test
    fun testBigIntSqrt() {
        val num = BigInteger(300, Random())
        val square = num*num
        assertTrue(sqrt(square) == num)
    }

    @Test
    fun testBezout () {
        var attempts = 0
        while (attempts++ < 50) {
            val n = BigInteger(1024, 1, SecureRandom())
            val a = BigInteger(1024, SecureRandom())
            val b = calculateInverse(a, n)
            assertTrue(b.times(a).mod(n) == BigInteger.ONE)
        }
    }
}