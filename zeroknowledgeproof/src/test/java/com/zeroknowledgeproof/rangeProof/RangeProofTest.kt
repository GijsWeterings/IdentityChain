package com.zeroknowledgeproof.rangeProof

import junit.framework.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger
import java.security.SecureRandom

class RangeProofTest {
    @Test
    fun completeProcedure() {
        var attempts = 0
        while (attempts++ < 3) {
            val tp = RangeProofTrustedParty()
            val res = tp.runInteractiveProver(19, 18, 100) // I am 19 years old.
            assertTrue(res)
            println("Great success in interactive rangeproof")
        }
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