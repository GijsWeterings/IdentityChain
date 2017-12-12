package com.zeroknowledgeproof.numberProof

import android.provider.Settings
import com.zeroknowledgeproof.SecretOrderGroupGenerator
import com.zeroknowledgeproof.rangeProof.RangeProofTrustedParty
import com.zeroknowledgeproof.rangeProof.calculateInverse
import junit.framework.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger
import java.security.SecureRandom

class RangeProofTest {
    @Test
    fun completeProcedure() {
        val tp = RangeProofTrustedParty
        val res = tp.genRangeProof(19, 18, 100) // I am 19 years old.
        assertTrue(res)
    }

    @Test
    fun testBezout () {
        var attempt = 0;
        while (attempt < 50) {
            attempt++
            val n = BigInteger(1024, 50,SecureRandom())
            val a = BigInteger(1024, SecureRandom())
            val b = calculateInverse(a, n)
            assertTrue(b.times(a).mod(n) == BigInteger.ONE)
        }
    }
}