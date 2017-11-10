package nl.tudelft.cs4160.trustchain_android.Util;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static nl.tudelft.cs4160.trustchain_android.Util.Util.ellipsize;
import static org.junit.Assert.*;

/**
 * Created by meijer on 10-11-17.
 */
@RunWith(JUnit4.class)
public class UtilTest {
    @Test
    public void ellipsizetest() throws Exception {
        String input = "12345678910";
        String expected = "1(..)0";
        Assert.assertEquals(expected,ellipsize(input,5));
    }

    @Test
    public void ellipsizetest2() throws Exception {
        String input = "12345678910";
        String expected = "12(..)10";
        Assert.assertEquals(expected,ellipsize(input,8));
    }

    @Test
    public void ellipsizetest3() throws Exception {
        String input = "12345678910";
        Assert.assertEquals(input,ellipsize(input,11));
    }

    @Test
    public void ellipsizetest4() throws Exception {
        String input = "12345678910";
        String expected = "1(..)0";
        Assert.assertEquals(expected,ellipsize(input,6));
    }

    @Test
    public void ellipsizetest5() throws Exception {
        String input = "12";
        String expected = "12";
        Assert.assertEquals(expected,ellipsize(input,5));
    }

}