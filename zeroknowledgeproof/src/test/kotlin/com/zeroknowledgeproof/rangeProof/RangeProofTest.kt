package com.zeroknowledgeproof.rangeProof

import org.junit.Assert.assertTrue
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
        }
    }

    @Test(expected = ZeroKnowledgeException::class)
    fun underRange() {
        val tp = RangeProofTrustedParty()
        tp.runInteractiveProver(17, 18, 100) // I am 17 years old.
    }


    @Test(expected = ZeroKnowledgeException::class)
    fun aboveRange() {
        val tp = RangeProofTrustedParty()
        tp.runInteractiveProver(101, 18, 100) // I am 101 years old.
    }

    @Test
    fun onLowerBoundRange() {
        var attempts = 0
        while (attempts++ < 3) {
            val tp = RangeProofTrustedParty()
            val res = tp.runInteractiveProver(18, 18, 100) // I am 18 years old.
            assertTrue(res)
        }
    }
    @Test
    fun onUpperBoundRange() {
        var attempts = 0
        while (attempts++ < 3) {
            val tp = RangeProofTrustedParty()
            val res = tp.runInteractiveProver(100, 18, 100) // I am 100 years old.
            assertTrue(res)
        }
    }
}