package com.zeroknowledgeproof.rangeProof

import java.math.BigInteger
import java.math.BigInteger.ONE
import java.math.BigInteger.ZERO
import java.security.MessageDigest
import java.security.Security

class RangeProofVerifier (givenN: BigInteger, givenLow: Int, givenUp: Int) {
    private val N = givenN
    private val low = givenLow
    private val up = givenUp

    fun requestChallenge (bound: BigInteger) : Pair<BigInteger, BigInteger> {
        val s = generateRandomInterval(ONE, bound)
        val t = generateRandomInterval(ONE, bound)
        return if (s == t)
            requestChallenge(bound)
        else
            Pair(s, t)
    }

    fun verify(res: interactiveProof, ver: commitVerification): Boolean {

        println("Verifier should check all results")
        // Step 7: NumberProofVerifier verifies ALL of these claims.

        verifyTwoCommittments(ver)

        if(res.rpr.c1.mod(N) != res.rpr.c.times(calculateInverse(res.rpr.g.modPow(toBigInt(low - 1), N), N)).mod(N)) {
            throw ZeroKnowledgeException("c1 = c/g^(a-1) mod N failed")
        } else if(res.rpr.c2.mod(N) != res.rpr.g.modPow(toBigInt(up + 1), N).times(calculateInverse(res.rpr.c, N)).mod(N)) {
            throw ZeroKnowledgeException("c2 = g^(b+1)/c mod N failed")
        } else if(res.rpr.cDPrime.mod(N) != res.rpr.c1Prime.times(res.rpr.c2Prime).times(res.rpr.c3Prime).mod(N)) {
            throw ZeroKnowledgeException("c''= c1'c2'c3' mod N failed")
        } else if(res.rpr.c1Prime.modPow(res.s, N).times(res.rpr.c2Prime).times(res.rpr.c3Prime).mod(N) != res.rpr.g.modPow(res.x, N).times(res.rpr.h.modPow(res.u, N)).mod(N)){
            throw ZeroKnowledgeException("c1'^s*c2'*c3' = g^x*h^u mod N failed")
        } else if (res.rpr.c1Prime.times(res.rpr.c2Prime.modPow(res.t, N)).times(res.rpr.c3Prime).mod(N) != res.rpr.g.modPow(res.y, N).times(res.rpr.h.modPow(res.v, N)).mod(N)){
            throw ZeroKnowledgeException("c1'*c2'^t*c3' = g^y*h^v mod N failed")
        } else if(res.x <= ZERO){
            throw ZeroKnowledgeException("x > 0 failed")
        } else if(res.y <= ZERO){
            throw ZeroKnowledgeException("y > 0 failed")
        }

        return true
    }

    private fun verifyTwoCommittments(ver: commitVerification){
        Security.insertProviderAt(org.spongycastle.jce.provider.BouncyCastleProvider(), 1)
        val messageDigest = MessageDigest.getInstance("SHA-512")

        val W1 = ver.g1.modPow(ver.D,N).times(ver.h1.modPow(ver.D1,N)).times(ver.E.modPow(-ver.c, N)).mod(N)
        val W2 = ver.g2.modPow(ver.D,N).times(ver.h2.modPow(ver.D2,N)).times(ver.F.modPow(-ver.c, N)).mod(N)

        if(ver.c != BigInteger(messageDigest.digest(W1.toByteArray() + W2.toByteArray()))){
            throw ZeroKnowledgeException("It can not be verified that the two committments hide the same secret")
        }
    }
}