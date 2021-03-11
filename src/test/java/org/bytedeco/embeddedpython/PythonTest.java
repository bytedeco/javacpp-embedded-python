package org.bytedeco.embeddedpython;

import org.junit.Test;
import scala.Function2;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class PythonTest {
    @Test
    public void testBasic() {
        double a = 355;
        double b = 113;
        Python.put("a", a);
        Python.put("b", b);
        Python.exec("v = a / b");
        assertEquals(a / b, Python.get("v"), 1e-10);
        Python.clear();
    }

    @Test
    public void testLambda() {
        //noinspection Convert2MethodRef
        Python.put("f", (Function2<Long, Long, Long>) (a, b) -> a + b);
        long v = Python.eval("f(1, 2)");
        assertEquals(3L, v);
    }

    @Test
    public void testStringArray() {
        Python.put("v", new String[]{"foo", "bar"});
        Object[] v = Python.get("v");
        assertEquals("foo", v[0]);
        assertEquals("bar", v[1]);
    }

    @Test
    public void testIterable() {
        Python.put("v", Arrays.asList("foo", 123));
        Object[] v = Python.get("v");
        assertEquals("foo", v[0]);
        assertEquals(123L, v[1]);
    }

    @Test
    public void testFloatArray2d() {
        Python.exec("import numpy as np");

        double v = Python.eval("10.0");
        assertEquals(10d, v, 1e-10d);

        float ndary0 = Python.eval("np.float32(10)");
        assertEquals(10f, ndary0, 1e-10f);

        NpNdarrayFloat ndary1 = Python.eval("np.arange(3, dtype=np.float32)");
        float[] ary1 = ndary1.toArray();
        assertEquals(3, ary1.length);
        assertEquals(2f, ary1[2], 1e-10f);

        NpNdarrayFloat ndary2 = Python.eval("np.arange(6, dtype=np.float32).reshape([2, 3])");
        float[][] ary2 = ndary2.toArray2d();
        assertEquals(2, ary2.length);
        assertEquals(3, ary2[0].length);
        assertEquals(3f, ary2[1][0], 1e-10f);

        NpNdarrayChar ndary3 = Python.eval("np.arange(24, dtype=np.uint16).reshape([2, 3, 4])");
        char[][][] ary3 = ndary3.toArray3d();
        assertEquals(2, ary3.length);
        assertEquals(3, ary3[0].length);
        assertEquals(4, ary3[0][0].length);
        assertEquals(17, ary3[1][1][1]);
    }

    @Test(expected = PythonException.class)
    public void testException() {
        Python.clear();
        Python.eval("a + 1");
    }
}
