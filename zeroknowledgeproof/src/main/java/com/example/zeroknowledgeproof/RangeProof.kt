package com.example.zeroknowledgeproof

// An implementation based on An Efficient Range Proof Scheme by Kun Peng and Feng Bao

class RangeProof () {
    /**
     * Generate a proof that a given number is in a range
     */
    fun Generator (m: Int, lowerBound: Int, upperBound: Int) : String {
        // Prove that boundedNumber is between lower and upper bound
        if ((m - lowerBound + 1)*(upperBound - m + 1) <= 0) {
            throw IllegalArgumentException("Cannot generate proof, because this is just NOT TRUE")
        }



    }
}