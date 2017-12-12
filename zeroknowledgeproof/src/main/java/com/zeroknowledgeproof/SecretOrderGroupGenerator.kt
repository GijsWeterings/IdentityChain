/*
 * Copyright 2017 ING Bank N.V.
 * This file is part of the go-ethereum library.
 *
 * The go-ethereum library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The go-ethereum library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the go-ethereum library. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.zeroknowledgeproof

import java.math.BigInteger
import java.security.SecureRandom
import java.math.BigInteger.ONE

class SecretOrderGroupGenerator(private val bitLength: Int, private val certainty: Int) {

    @JvmOverloads constructor(bitlength: Int = 1024) : this(bitlength, 50) {}

    fun generate(): SecretOrderGroup {

        val safePrimes = generateSafePrimes(bitLength, certainty)
        val generators = findGenerators(safePrimes)

        val N = safePrimes[0].multiply(safePrimes[1])

        return SecretOrderGroup(N, generators[0], generators[1])
    }

    companion object {
        private val TWO = BigInteger.valueOf(2)

        private val rnd = SecureRandom()


        fun generateSafePrimes(bitlength: Int, certainty: Int): Array<BigInteger> {
            val P = generateSafePrime(bitlength - 1, certainty)
            var result: BigInteger
            do {
                result = generateSafePrime(bitlength - 1, certainty)
            } while (result == P)

            return  arrayOf(P, result)
        }

        /**
         * Generates a safe prime P such that (P - 1) / 2 is also prime.
         */
        private fun generateSafePrime(bitlength: Int, certainty: Int): BigInteger {
            var bigPrime: BigInteger
            var smallPrime: BigInteger
            var attempts = 0
            do {
                attempts++
                bigPrime = BigInteger(bitlength, certainty, rnd)
                smallPrime = bigPrime.subtract(ONE).divide(TWO)

                if (attempts % 100 == 0) {
                    println("#attempts = " + attempts)
//                    LOGGER.debug("#attempts = " + attempts)
                }

                // check whether smallPrime is also prime, otherwise generate new bigPrime
            } while (!smallPrime.isProbablePrime(certainty))

//            LOGGER.debug("Found safe prime after $attempts attempts")
            return bigPrime
        }

        private fun createRandomInRange (start: BigInteger, end: BigInteger) : BigInteger {
            val range = end.subtract(start)
            var result: BigInteger = end.plus(ONE);
            while (result > range) {
                result = BigInteger(range.bitLength(), SecureRandom())
            }
            return result.plus(start)
        }

        // Find two generators of G_pq.
        // This is step 2 to 4 in the "Set-up procedure" in the paper from Fujisaki and Okamoto, page 19
        // Therefore the generators for G_pq are called b0, b1 instead of g, h
         fun findGenerators(safePrimes: Array<BigInteger>): Array<BigInteger> {
            val P = safePrimes[0]
            val Q = safePrimes[1]
            val p = safePrimes[0].subtract(ONE).divide(TWO)
            val q = safePrimes[1].subtract(ONE).divide(TWO)
            val N = safePrimes[0].multiply(safePrimes[1])

            // Step 2
            var g_p: BigInteger
            var g_q: BigInteger

            g_p = findGeneratorForSafePrime(P)
            g_p = g_p.modPow(createRandomInRange(ONE, p.subtract(ONE)), P)

            g_q = findGeneratorForSafePrime(Q)
            g_q = g_q.modPow(createRandomInRange(ONE, q.subtract(ONE)), Q)

            // Step 3
            val bezout = extendedGCDBezout(P, Q)
            val b0 = g_p.multiply(bezout[1]).multiply(Q).add(g_q.multiply(bezout[0]).multiply(P)).mod(N)

            // Step 4
            var alpha: BigInteger
            do {
                // We use min(p,q) as minimum for alpha, small numbers would make it easier to find log_b1(b0)
                alpha = createRandomInRange(p.min(q), p.multiply(q))

            } while (alpha.mod(p) == BigInteger.ZERO || alpha.mod(q) == BigInteger.ZERO)
            // At b1 := b0^alpha, alpha should not be a multiple of p or q, otherwise b1 only generates a small subgroup

            val b1 = b0.modPow(alpha, N)

            return arrayOf(b0, b1)
        }

        /* For the given safe prime P, find a generator for a subgroup of order (P - 1) / 2. */
        private fun findGeneratorForSafePrime(P: BigInteger): BigInteger {
            // If P is a safe prime with p = (P - 1) / 2, then generated groups modulo P have order 1, 2, p or 2p
            // According to Fujisaki and Okamoto we need a group of order p.
            // To know that a generator does not have order 1 or 2, we check g^2 != 1
            // To know that a generator does not have order 2p, we check that g^p == 1

            val p = P.subtract(ONE).divide(TWO)
            var g: BigInteger
            do {
                g = createRandomInRange(TWO, P.subtract(TWO))
            } while (g.modPow(p, P) != ONE || g.modPow(TWO, P) == ONE)

            return g
        }

        private fun extendedGCDBezout(a: BigInteger, b: BigInteger): Array<BigInteger> {
            var s0 = BigInteger.ONE
            var s1 = BigInteger.ZERO
            var t0 = BigInteger.ZERO
            var t1 = BigInteger.ONE
            var r0 = a
            var r1 = b
            var temp: BigInteger

            while (r1 != BigInteger.ZERO) {
                val quotient = r0.divideAndRemainder(r1)
                r0 = r1
                r1 = quotient[1]

                temp = s1
                s1 = s0.subtract(quotient[0].multiply(s1))
                s0 = temp

                temp = t1
                t1 = t0.subtract(quotient[0].multiply(t1))
                t0 = temp
            }

            return arrayOf(s0, t0)
        }
    }
}
