package nl.tudelft.cs4160.trustchain_android.Util

import org.junit.Test

import junit.framework.Assert.assertEquals
import nl.tudelft.cs4160.trustchain_android.Util.Util.ellipsize


class UtilTest {
    @Test
    fun ellipsizetest() {
        val input = "12345678910"
        val expected = "1(..)0"
        assertEquals(expected, ellipsize(input, 5))
    }

    @Test
    fun ellipsizetest2() {
        val input = "12345678910"
        val expected = "12(..)10"
        assertEquals(expected, ellipsize(input, 8))
    }

    @Test
    fun ellipsizetest3() {
        val input = "12345678910"
        assertEquals(input, ellipsize(input, 11))
    }

    @Test
    fun ellipsizetest4() {
        val input = "12345678910"
        val expected = "1(..)0"
        assertEquals(expected, ellipsize(input, 6))
    }

    @Test
    fun ellipsizetest5() {
        val input = "12"
        val expected = "12"
        assertEquals(expected, ellipsize(input, 5))
    }

}