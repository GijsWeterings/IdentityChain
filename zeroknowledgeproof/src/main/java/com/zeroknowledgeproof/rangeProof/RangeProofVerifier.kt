package com.zeroknowledgeproof.rangeProof

import java.math.BigInteger
import java.security.SecureRandom

class RangeProofVerifier (givenN: BigInteger) {
    private val debug = false
    private val N = givenN
    private val rand = SecureRandom()

    fun generateRandomInterval(lowerBound: BigInteger, upperBound: BigInteger): BigInteger {
        var res: BigInteger
        do {
            res = BigInteger(upperBound.bitLength(), rand)
        } while (res > upperBound || res == BigInteger.ZERO)
        return res
    }

    /**
     *
     */
    fun verify(res: RangeProofTrustedParty.rangeProofResult): Boolean {
        TODO("Not yet implemented")
        return false
    }
}