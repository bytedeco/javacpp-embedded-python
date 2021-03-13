package org.bytedeco.embeddedpython;

import org.junit.Test;
import scala.Function2;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;

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

        assertEquals("<built-in function org.bytedeco.embeddedpython>", Python.eval("str(f)"));
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
    public void testMap() {
        HashMap<String, List<Object>> map1 = new HashMap<>();
        map1.put("a", Arrays.asList(1, 2));
        map1.put("b", Arrays.asList(3, 4));
        Python.put("v", map1);

        HashMap<String, Object[]> map2 = Python.get("v");
        assertEquals(map1.keySet(), map2.keySet());
        assertArrayEquals(new Object[]{1L, 2L}, map2.get("a"));
        assertArrayEquals(new Object[]{3L, 4L}, map2.get("b"));
    }

    @Test
    public void testBytes() {
        byte[] ary1 = new byte[]{1, 2, 3};
        Python.put("v", ary1);
        byte[] ary2 = Python.get("v");
        assertArrayEquals(ary1, ary2);
    }

    @Test
    public void testByteArray() {
        byte[] ary1 = new byte[]{1, 2, 3};
        Python.put("v", ary1);
        byte[] ary2 = Python.eval("bytearray(v)");
        assertArrayEquals(ary1, ary2);
    }

    @Test
    public void testLongArray() {
        Python.exec("import numpy as np");

        long[] ary1 = new long[]{Long.MAX_VALUE - 1, Long.MAX_VALUE - 2};
        Python.put("v", ary1);
        NpNdarrayLong ndary2 = Python.get("v");
        NpNdarrayLong ndary3 = Python.eval("np.array(" + Arrays.toString(ary1) + ", dtype=np.int64)");
        assertArrayEquals(ary1, ndary2.toArray());
        assertArrayEquals(ary1, ndary3.toArray());
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
        assertEquals(ndary1, new NpNdarrayFloat(ary1));

        NpNdarrayFloat ndary2 = Python.eval("np.arange(6, dtype=np.float32).reshape([2, 3])");
        float[][] ary2 = ndary2.toArray2d();
        assertEquals(2, ary2.length);
        assertEquals(3, ary2[0].length);
        assertEquals(3f, ary2[1][0], 1e-10f);
        assertEquals(ndary2, new NpNdarrayFloat(ary2));

        NpNdarrayChar ndary3 = Python.eval("np.arange(24, dtype=np.uint16).reshape([2, 3, 4])");
        char[][][] ary3 = ndary3.toArray3d();
        assertEquals(2, ary3.length);
        assertEquals(3, ary3[0].length);
        assertEquals(4, ary3[0][0].length);
        assertEquals(17, ary3[1][1][1]);
        assertEquals(ndary3, new NpNdarrayChar(ary3));
    }

    @Test
    public void testBooleanNdarray() {
        Python.exec("import numpy as np");
        NpNdarrayBoolean npary = Python.eval("np.array([True, False])");
        assertArrayEquals(new boolean[]{true, false}, npary.toArray());
    }

    @Test(expected = PythonException.class)
    public void testException() {
        Python.clear();
        Python.eval("a + 1");
    }

    @Test(expected = PythonException.class)
    public void testSyntaxErrorEval() {
        Python.eval("print(Hello')");
    }

    @Test(expected = PythonException.class)
    public void testSyntaxErrorExec() {
        Python.exec("print(Hello')");
    }

    @Test
    public void testLambdaException() {
        Python.put("f", (Function2<Long, Long, Long>) (a, b) -> {
            throw new RuntimeException("From lambda");
        });
        Python.exec("try:\n" +
                "   f(1, 2)\n" +
                "   v = Flase\n" +
                "except RuntimeError as e:\n" +
                "   print(e)\n" +
                "   v = True\n");
        boolean v = Python.get("v");
        assertTrue(v);
    }

    @Test
    public void testDatetime() {
        Python.exec("import numpy as np");

        NpNdarrayInstant ndary1 = Python.eval("np.array(['2021-03-01T10:02:03'], dtype='datetime64[s]')");
        assertEquals("2021-03-01T10:02:03Z", ndary1.toArray()[0].toString());
        NpNdarrayInstant ndary2 = Python.eval("np.array(['2021-03-01T10:02:03'], dtype='datetime64[ms]')");
        assertEquals("2021-03-01T10:02:03Z", ndary2.toArray()[0].toString());
        NpNdarrayInstant ndary3 = Python.eval("np.array(['2021-03-01T10:02:03'], dtype='datetime64[us]')");
        assertEquals("2021-03-01T10:02:03Z", ndary3.toArray()[0].toString());
        NpNdarrayInstant ndary4 = Python.eval("np.array(['2021-03-01T10:02:03'], dtype='datetime64[ns]')");
        assertEquals("2021-03-01T10:02:03Z", ndary4.toArray()[0].toString());

        NpNdarrayInstant ndary5 = Python.eval("np.array(['2021-03-01T10:02:03'], dtype='datetime64[m]')");
        assertEquals("2021-03-01T10:02:00Z", ndary5.toArray()[0].toString());
        NpNdarrayInstant ndary6 = Python.eval("np.array(['2021-03-01T10:02:03'], dtype='datetime64[h]')");
        assertEquals("2021-03-01T10:00:00Z", ndary6.toArray()[0].toString());
        NpNdarrayInstant ndary7 = Python.eval("np.array(['2021-03-01T10:02:03'], dtype='datetime64[D]')");
        assertEquals("2021-03-01T00:00:00Z", ndary7.toArray()[0].toString());
        NpNdarrayInstant ndary8 = Python.eval("np.array(['2021-03-01T10:02:03'], dtype='datetime64[W]')");
        assertEquals("2021-02-25T00:00:00Z", ndary8.toArray()[0].toString());

        Python.put("v", ndary4);
        Python.exec("print(v)");
        NpNdarrayInstant ndary9 = Python.get("v");
        assertEquals(ndary4, ndary9);

        Python.put("v", ndary4.data);
        Python.exec("print(v)");
        NpNdarrayInstant ndary10 = Python.get("v");
        assertEquals(ndary4, ndary10);

        Python.put("v", ndary4.data[0]);
        Python.exec("print(v)");
        Instant instant11 = Python.get("v");
        assertEquals(ndary4.data[0], instant11);
    }

    @Test
    public void testStdoutBuffering() {
        System.out.println("1");
        Python.exec("print(2)");
        System.out.println("3");
    }
}
