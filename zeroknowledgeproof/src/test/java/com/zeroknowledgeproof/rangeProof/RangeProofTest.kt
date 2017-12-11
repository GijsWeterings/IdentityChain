package com.zeroknowledgeproof.numberProof

import com.zeroknowledgeproof.rangeProof.RangeProofTrustedParty
import junit.framework.Assert.assertTrue
import org.junit.Test

class RangeProofTest {
    @Test
    fun completeProcedure() {
        val tp = RangeProofTrustedParty
        val res = tp.genRangeProof(19, 18, 100) // I am 19 years old.
        assertTrue(res)
    }
}