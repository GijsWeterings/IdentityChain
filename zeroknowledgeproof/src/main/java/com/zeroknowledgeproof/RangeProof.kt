package com.zeroknowledgeproof

import java.math.BigInteger
import java.security.SecureRandom
import com.zeroknowledgeproof.SecretOrderGroupGenerator.Companion.findGenerators
import com.zeroknowledgeproof.SecretOrderGroupGenerator.Companion.generateSafePrimes
import java.lang.Math.floor
import java.lang.Math.sqrt
import java.math.BigInteger.*
import java.util.*


// An implementation based on An Efficient Range Proof Scheme by Kun Peng and Feng Bao



class RangeProof () {
    /**
     * Generate a proof that a given number is in a range
     */
    val g = BigInteger("5866666666666666666666666666666666666666666666666666666666666666", 16)
    var s = BigInteger.ZERO
    var t = BigInteger.ZERO
    val rand = SecureRandom(); // SEKURE random

    fun Prover (m: Int, a: Int, b: Int) : Boolean {
        // Prove that boundedNumber is between lower and upper bound
        if ((m - a + 1)*(b - m + 1) <= 0) {
            throw IllegalArgumentException("Cannot generate proof, because this is just NOT TRUE")
        }


        val w = rand.nextInt(10000)

        val theProof = toBigInt(w * w).times(toBigInt(m - a + 1)).times(toBigInt(b - m + 1))

        val m4 = generateToSum(toBigInt(sqrt(theProof.toDouble()).toInt()))
        val m3 = m4.times(m4);
        val m2 = generateToSum(theProof.subtract(m3)) //Take a random int of interval [0, theProof-m3]
        val m1 = theProof.subtract(m3).subtract(m2)      // Now m1 + m2 + m3 = w^2 * (m-a+1) * (b-m+1)

//        var P: BigInteger
//        var Q: BigInteger
        var p: BigInteger
        var q: BigInteger
//        var PminOne: BigInteger

//        var attempts = 0;
//        do {
//            P = BigInteger.probablePrime(2048, rand)
//            attempts++
//            if (attempts % 25 == 0) {
//                println("Calculating P and p, now at " + attempts + " attempts.")
//            }
//            PminOne = P.subtract(ONE).divide(BigInteger.valueOf(2))
//        } while (!PminOne.isProbablePrime(50) )

          var QPprimes = generateSafePrimes(2048, 50)
            val P = QPprimes[0]
            val Q = QPprimes[1]

//        do {
//            Q = BigInteger.probablePrime(2048, rand)
//            attempts++
//            if (attempts % 25 == 0) {
//                println("Calculating Q and q now at " + attempts + " attempts.")
//            }
//        } while (!Q.subtract(ONE).divide(BigInteger.valueOf(2)).isProbablePrime(50) || P.equals(Q))

        val N = P.multiply(Q)

        val k1 = BigInteger(1024, rand)
        val k2 = BigInteger(160, rand)


        val (g,h) = findGenerators(findTwoPrimes(2048))

        val r = BigInteger(N.bitLength(), rand)

        val c = g.modPow(toBigInt(m), N).times(h.modPow(r, N)).mod(N)

        val c1 = c.divide(g.modPow(toBigInt(a-1), N)).mod(N) // c1 = c/(g^a-1) modN
        //TODO Maybe this should not be Integer division, but cyclic group division (groot crypto fun)
        val c2 = g.modPow(toBigInt(b+1), N).divide(c).mod(N)
        val rPrime = BigInteger(N.bitLength(), rand)

        val cPrime = c1.modPow(toBigInt(b - m + 1), N).times(h.modPow(rPrime, N)).mod(N)
        val rDPrime = BigInteger(N.bitLength(), rand)

        val cDPrime = cPrime.modPow(toBigInt(w*w), N).times(h.modPow(rDPrime, N)).mod(N)

        val publicProof = cPrime.toString(10) + "," + h.toString(10)
        print("The Public Proof is " + publicProof)

        // Prove that the hidden number is a square with base (c', h)
        //TODO: implement, ING used a lot of classes for this

        val rSum = toBigInt((w*w)*((b-m+1))).times(r) + rPrime + rDPrime

        val r1 = BigInteger(160, rand)
        val r2 = BigInteger(160, rand)
        val r3 = rSum-r1-r2                     //r1 + r2 + r3 = rSum

        val c1Prime = g.modPow(m1, N).times(h.modPow(r1,N)).mod(N)
        val c2Prime = g.modPow(m2, N).times(h.modPow(r2,N)).mod(N)
        val c3Prime = cDPrime.divide(c1Prime).times(c2Prime).mod(N)


        val x = s.times(m1).plus(m2 + m3)
        val y = m1.plus(m3).plus(t.times(m2))
        val u = s.times(r1).plus(r2 + r3)
        val v = r1.plus(r3).plus(t.times(r2))

        //TODO: Publish x,y,u,v


        //verify
//
//        if(!c1.equals(c.divide(g.modPow(toBigInt(a-1), N)).mod(N)) ||
//                !c2.equals(g.modPow(toBigInt(b+1), N).divide(c).mod(N)) ||
//                !cDPrime.equals(c1Prime.times(c2Prime).times(c3Prime).mod(N)) ||
//                !pow(c1Prime,s).times(c2Prime).times(c3Prime).equals(g.mod(x).times(pow(h,u)).mod(N)) ||
//                !pow(c2Prime,t).times(c1Prime).times(c3Prime).equals(g.mod(y).times(pow(h,v)).mod(N)) ||
//                x < ZERO ||
//                y < ZERO){
//            throw Exception("Verify gone wrong")
//        }

        //This structure makes debugging easier as you can set breakpoints on all the cases :)
        if(!c1.equals(c.divide(g.modPow(toBigInt(a-1), N)).mod(N))){
            println("first")
        } else if(!c2.equals(g.modPow(toBigInt(b+1), N).divide(c).mod(N))){
            println("2")
        }
        else if(!cDPrime.equals(c1Prime.times(c2Prime).times(c3Prime).mod(N))) {
            println("3")
        }
        else if(!pow(c1Prime,s).times(c2Prime).times(c3Prime).equals(g.mod(x).times(pow(h,u)).mod(N))) {
            println("4")
        }
        else if(!pow(c2Prime,t).times(c1Prime).times(c3Prime).equals(g.mod(y).times(pow(h,v)).mod(N))) {
            println("5")
        }
        else if(x < ZERO) {
            println("x zero")
        }
        else if(y < ZERO) {
            println("y zero")
        }


        return true;
    }

    fun verifier(k1 : BigInteger) {
        s = generateInCyclicGroup(k1)
        t = generateInCyclicGroup(k1)
    }


    private fun generateInCyclicGroup(k : BigInteger) : BigInteger{
        var temp = BigInteger(k.bitLength(),rand)
        temp = temp.mod(k)

        if(temp.equals(ZERO)){
            temp = generateInCyclicGroup(k)
        }

        return temp
    }

    private fun toBigInt(i: Int) : BigInteger {
        return BigInteger.valueOf(i.toLong())
    }

    //from: https://stackoverflow.com/questions/4582277/biginteger-powbiginteger
    fun pow(base: BigInteger, exponent: BigInteger): BigInteger {
        var base = base
        var exponent = exponent
        var result = BigInteger.ONE
        while (exponent.signum() > 0) {
            if (exponent.testBit(0)) result = result.multiply(base)
            base = base.multiply(base)
            exponent = exponent.shiftRight(1)
        }
        return result
    }

    private fun generateToSum(sum: BigInteger): BigInteger {
        var result: BigInteger = sum.plus(ONE);
        while (result > sum) {
            result = BigInteger(sum.bitLength(), SecureRandom())
        }
        return result
    }

    private fun findTwoPrimes(length: Int) : Array<BigInteger> {
        var first = BigInteger.probablePrime(length,rand)
        var second : BigInteger
        do {
            second =  BigInteger.probablePrime(length,rand)
        }
        while (second.equals(first))

        return arrayOf(first,second)


    }

//     Randomly choose m1, m2, m4 smaller than (non-negative) sum, such that m1 + m2 + m4^2 = sum
//    fun takeRandomM(sum: BigInteger): Array<BigInteger> {
//        val random = SecureRandom()
//        val maxForM4 = BigIntUtil.floorSquareRoot(sum)
//        val m4 = BigInteger.createRandomInRange(ZERO, maxForM4, random)
//        val remaining = sum.subtract(m4.multiply(m4))
//        val m1 = BigInteger.createRandomInRange(ZERO, remaining, random)
//        val m2 = remaining.subtract(m1)
//        return arrayOf(m1, m2, m4)
//    }
}