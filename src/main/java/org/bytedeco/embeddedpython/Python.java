package org.bytedeco.embeddedpython;

import org.bytedeco.cpython.PyCFunction;
import org.bytedeco.cpython.PyMethodDef;
import org.bytedeco.cpython.PyObject;
import org.bytedeco.cpython.PyTypeObject;
import org.bytedeco.cpython.global.python;
import org.bytedeco.javacpp.*;
import org.bytedeco.numpy.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.StreamSupport;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.bytedeco.cpython.global.python.*;
import static org.bytedeco.cpython.helper.python.Py_AddPath;
import static org.bytedeco.cpython.presets.python.cachePackages;
import static org.bytedeco.numpy.global.numpy.*;

/**
 * Python interpreter.
 * <p>
 * If you embed two Python interpreters, many Python libraries do not work correctly.
 * Therefore this class is a singleton class. All the methods are static.
 * <p>
 * This class is thread-safe. All the methods are synchronized.
 */
public class Python {
    static {
        try {
            init();
        } catch (Exception e) {
            throw new PythonException("Failed at Python.init()", e);
        }
    }

    private static void init() throws IOException {
        System.setProperty("org.bytedeco.openblas.load", "mkl");

        Py_AddPath(cachePackages());
        Py_AddPath(org.bytedeco.numpy.presets.numpy.cachePackages());

        Py_Initialize();
        _import_array();

        Runtime.getRuntime().addShutdownHook(new Thread(python::Py_Finalize));
    }

    private static final PyObject mainModule = PyImport_AddModule("__main__");
    private static final PyObject globals = PyModule_GetDict(mainModule);

    private Python() {
    }

    /**
     * Delete all the global variables.
     */
    public synchronized static void clear() {
        PyDict_Clear(globals);
    }

    /**
     * Don't forget to call Py_DecRef().
     */
    private static PyObject compile(String src) {
        PyObject co = Py_CompileString(src, "<string>", Py_eval_input);
        if (co == null) {
            throw new PythonException("Py_CompileString() failed. src = " + src);
        }
        return co;
    }

    /**
     * Python built-in eval().
     *
     * @param src Python code. This must be a single line code.
     */
    @SuppressWarnings("unchecked")
    public synchronized static <T> T eval(String src) {
        PyObject co = compile(src);
        try {
            PyObject obj = PyEval_EvalCode(co, globals, globals);
            try {
                if (obj == null) {
                    if (PyErr_Occurred() != null) {
                        PyErr_Print();
                    }
                    throw new PythonException("PyEval_EvalCode() failed. An Error is thrown inside Python. src = " + src);
                }
                return (T) toJava(obj);
            } finally {
                Py_DecRef(obj);
            }
        } finally {
            Py_DecRef(co);
        }
    }

    /**
     * Python built-in exec().
     *
     * @param src Python code. This can be multiple lines code.
     */
    public synchronized static void exec(String src) {
        if (PyRun_SimpleStringFlags(src, null) != 0) {
            throw new PythonException("PyRun_SimpleStringFlags() failed. src = " + src);
        }
    }

    /**
     * Get the global Python variable and convert it to a Java object.
     * <p>
     * <table border="1">
     * <caption>Type mappings. Python to Java.</caption>
     * <thead><tr><th>Python</th><th>Java</th></tr></thead>
     * <tbody>
     * <tr><td>None</td><td>null</td></tr>
     * <tr><td>bool</td><td>boolean</td></tr>
     * <tr><td>int</td><td>long</td></tr>
     * <tr><td>float</td><td>double</td></tr>
     * <tr><td>str</td><td>String</td></tr>
     * <tr><td>bytes</td><td>byte[]</td></tr>
     * <tr><td>bytearray</td><td>byte[]</td></tr>
     * <tr><td>Tuple</td><td>Object[]</td></tr>
     * <tr><td>List</td><td>Object[]</td></tr>
     * <tr><td>Dict</td><td>Map</td></tr>
     * <tr><td>scalar np.bool8</td><td>boolean</td></tr>
     * <tr><td>scalar np.int8</td><td>byte</td></tr>
     * <tr><td>scalar np.int16</td><td>short</td></tr>
     * <tr><td>scalar np.uint16</td><td>char</td></tr>
     * <tr><td>scalar np.int32</td><td>int</td></tr>
     * <tr><td>scalar np.int64</td><td>long</td></tr>
     * <tr><td>scalar np.float32</td><td>float</td></tr>
     * <tr><td>scalar np.float64</td><td>double</td></tr>
     * <tr><td>scalar np.datetime64[W, D, h, m, s, ms, us, or ns]</td><td>Instantr</td></tr>
     * <tr><td>ndarray np.bool8</td><td>NpNdarrayBoolean</td></tr>
     * <tr><td>ndarray np.int8</td><td>NpNdarrayByte</td></tr>
     * <tr><td>ndarray np.int16</td><td>NpNdarrayShort</td></tr>
     * <tr><td>ndarray np.uint16</td><td>NpNdarrayChar</td></tr>
     * <tr><td>ndarray np.int32</td><td>NpNdarrayInt</td></tr>
     * <tr><td>ndarray np.int64</td><td>NpNdarrayLong</td></tr>
     * <tr><td>ndarray np.float32</td><td>NpNdarrayFloat</td></tr>
     * <tr><td>ndarray np.float64</td><td>NpNdarrayDouble</td></tr>
     * <tr><td>ndarray np.datetime64[W, D, h, m, s, ms, us, or ns]</td><td>NpNdarrayInstant</td></tr>
     * </tbody>
     * </table>
     *
     * @param name The variable name
     * @throws PythonException        If the value cannot convert to a Java object.
     * @throws NoSuchElementException If the variable does not exists.
     */
    @SuppressWarnings("unchecked")
    public synchronized static <T> T get(String name) {
        return (T) toJava(getPyObject(name));
    }

    private static PyObject getPyObject(String name) {
        PyObject obj = PyDict_GetItemString(globals, name);
        if (obj == null) throw new NoSuchElementException("name = " + name);
        return obj;
    }

    /**
     * Convert the Java object and set it to the global Python variable.
     * <p>
     * <table border="1">
     * <caption>Type mappings. Java to Python.</caption>
     * <thead><tr><th>Java</th><th>Python</th></tr></thead>
     * <tbody>
     * <tr><td>null</td><td>None</td></tr>
     * <tr><td>boolean</td><td>bool</td></tr>
     * <tr><td>byte</td><td>int</td></tr>
     * <tr><td>short</td><td>int</td></tr>
     * <tr><td>char</td><td>int</td></tr>
     * <tr><td>int</td><td>int</td></tr>
     * <tr><td>long</td><td>int</td></tr>
     * <tr><td>float</td><td>float</td></tr>
     * <tr><td>double</td><td>float</td></tr>
     * <tr><td>Instant</td><td>np.datetime64[ms]</td></tr>
     * <tr><td>String</td><td>str</td></tr>
     * <tr><td>Iterable</td><td>List</td></tr>
     * <tr><td>Map</td><td>Dict</td></tr>
     * <tr><td>byte[]</td><td>bytes</td></tr>
     * <tr><td>boolean[]</td><td>np.ndarray, dtype=np.bool8</td></tr>
     * <tr><td>short[]</td><td>np.ndarray, dtype=np.int16</td></tr>
     * <tr><td>char[]</td><td>np.ndarray, dtype=np.uint16</td></tr>
     * <tr><td>int[]</td><td>np.ndarray, dtype=np.int32</td></tr>
     * <tr><td>long[]</td><td>np.ndarray, dtype=np.int64</td></tr>
     * <tr><td>float[]</td><td>np.ndarray, dtype=np.float32</td></tr>
     * <tr><td>double[]</td><td>np.ndarray, dtype=np.float64</td></tr>
     * <tr><td>Instant[]</td><td>np.ndarray, dtype=np.datetime64[ms]</td></tr>
     * <tr><td>NpNdarrayBoolean</td><td>np.ndarray, dtype=np.bool8</td></tr>
     * <tr><td>NpNdarrayByte</td><td>np.ndarray, dtype=np.int8</td></tr>
     * <tr><td>NpNdarrayShort</td><td>np.ndarray, dtype=np.int16</td></tr>
     * <tr><td>NpNdarrayChar</td><td>np.ndarray, dtype=np.uint16</td></tr>
     * <tr><td>NpNdarrayInt</td><td>np.ndarray, dtype=np.int32</td></tr>
     * <tr><td>NpNdarrayLong</td><td>np.ndarray, dtype=np.int64</td></tr>
     * <tr><td>NpNdarrayFloat</td><td>np.ndarray, dtype=np.float32</td></tr>
     * <tr><td>NpNdarrayDouble</td><td>np.ndarray, dtype=np.float64</td></tr>
     * <tr><td>NpNdarrayInstant</td><td>np.ndarray, dtype=np.datetime64[ms]</td></tr>
     * <tr><td>scala.Function0 - Function22</td><td>global Python function</td></tr>
     * </tbody>
     * </table>
     *
     * @param name  The variable name
     * @param value The value to put.
     * @throws PythonException If the value cannot convert to a Python object.
     */
    public synchronized static void put(String name, Object value) {
        putPyObject(name, toPyObject(value));
    }

    private static void putPyObject(String name, PyObject obj) {
        try {
            if (PyDict_SetItemString(globals, name, obj) != 0) {
                throw new PythonException("PyDict_SetItemString() failed");
            }
        } finally {
            Py_DecRef(obj);
        }
    }

    private static final PyTypeObject noneType = _PyNone_Type();
    private static final PyTypeObject boolType = PyBool_Type();
    private static final PyTypeObject longType = PyLong_Type();
    private static final PyTypeObject floatType = PyFloat_Type();
    private static final PyTypeObject unicodeType = PyUnicode_Type();
    private static final PyTypeObject bytesType = PyBytes_Type();
    private static final PyTypeObject byteArrayType = PyByteArray_Type();
    private static final PyTypeObject tupleType = PyTuple_Type();
    private static final PyTypeObject listType = PyList_Type();
    private static final PyTypeObject dictType = PyDict_Type();
    private static final PyTypeObject boolArrType = PyBoolArrType_Type();
    private static final PyTypeObject byteArrType = PyByteArrType_Type();
    private static final PyTypeObject ushortArrType = PyUShortArrType_Type();
    private static final PyTypeObject shortArrType = PyShortArrType_Type();
    private static final PyTypeObject intArrType = PyIntArrType_Type();
    private static final PyTypeObject longArrType = PyLongArrType_Type();
    private static final PyTypeObject floatArrType = PyFloatArrType_Type();
    private static final PyTypeObject doubleArrType = PyDoubleArrType_Type();
    private static final PyTypeObject datetimeArrType = PyDatetimeArrType_Type();
    private static final PyTypeObject arrayType = PyArray_Type();

    private static Object toJava(PyObject obj) {
        PyTypeObject t = obj.ob_type();
        if (t.equals(noneType)) {
            return null;
        } else if (t.equals(boolType)) {
            return PyLong_AsLong(obj) != 0;
        } else if (t.equals(longType)) {
            return PyLong_AsLong(obj);
        } else if (t.equals(floatType)) {
            return PyFloat_AsDouble(obj);
        } else if (t.equals(unicodeType)) {
            return new BytePointer(PyUnicode_AsUTF8(obj)).getString(UTF_8);
        } else if (t.equals(bytesType)) {
            byte[] ary = new byte[lengthToInt(PyBytes_Size(obj))];
            new BytePointer(PyBytes_AsString(obj)).get(ary);
            return ary;
        } else if (t.equals(byteArrayType)) {
            byte[] ary = new byte[lengthToInt(PyByteArray_Size(obj))];
            new BytePointer(PyByteArray_AsString(obj)).get(ary);
            return ary;
        } else if (t.equals(tupleType)) {
            Object[] ary = new Object[lengthToInt(PyTuple_Size(obj))];
            for (int i = 0; i < ary.length; i++) {
                ary[i] = toJava(PyTuple_GetItem(obj, i));
            }
            return ary;
        } else if (t.equals(listType)) {
            Object[] ary = new Object[lengthToInt(PyList_Size(obj))];
            for (int i = 0; i < ary.length; i++) {
                ary[i] = toJava(PyList_GetItem(obj, i));
            }
            return ary;
        } else if (t.equals(dictType)) {
            SizeTPointer pos = new SizeTPointer(1).put(0);
            HashMap<Object, Object> map = new HashMap<>();
            while (true) {
                PyObject key = new PyObject();
                PyObject value = new PyObject();
                int ok = PyDict_Next(obj, pos, key, value);
                if (ok == 0) break;
                map.put(toJava(key), toJava(value));
            }
            return map;
        } else if (t.equals(boolArrType)) {
            return new PyBoolScalarObject(obj).obval() != 0;
        } else if (t.equals(byteArrType)) {
            return new PyByteScalarObject(obj).obval();
        } else if (t.equals(ushortArrType)) {
            return (char) (new PyUShortScalarObject(obj).obval());
        } else if (t.equals(shortArrType)) {
            return new PyShortScalarObject(obj).obval();
        } else if (t.equals(intArrType)) {
            return new PyIntScalarObject(obj).obval();
        } else if (t.equals(longArrType)) {
            return new PyLongScalarObject(obj).obval();
        } else if (t.equals(floatArrType)) {
            return new PyFloatScalarObject(obj).obval();
        } else if (t.equals(doubleArrType)) {
            return new PyDoubleScalarObject(obj).obval();
        } else if (t.equals(datetimeArrType)) {
            PyDatetimeScalarObject datetimeScalarObj = new PyDatetimeScalarObject(obj);
            int datetimteUnit = datetimeScalarObj.obmeta().base();
            switch (datetimteUnit) {
                case NPY_FR_W:
                    return Instant.ofEpochSecond(datetimeScalarObj.obval() * (7L * 24L * 60L * 60L));
                case NPY_FR_D:
                    return Instant.ofEpochSecond(datetimeScalarObj.obval() * (24L * 60L * 60L));
                case NPY_FR_h:
                    return Instant.ofEpochSecond(datetimeScalarObj.obval() * (60L * 60L));
                case NPY_FR_m:
                    return Instant.ofEpochSecond(datetimeScalarObj.obval() * 60L);
                case NPY_FR_s:
                    return Instant.ofEpochSecond(datetimeScalarObj.obval());
                case NPY_FR_ms:
                    return Instant.ofEpochMilli(datetimeScalarObj.obval());
                case NPY_FR_us:
                    return Instant.ofEpochSecond(
                            datetimeScalarObj.obval() / (1000L * 1000L),
                            datetimeScalarObj.obval() % (1000L * 1000L));
                case NPY_FR_ns:
                    return Instant.ofEpochSecond(
                            datetimeScalarObj.obval() / (1000L * 1000L * 1000L),
                            datetimeScalarObj.obval() % (1000L * 1000L * 1000L));
                default:
                    throw new RuntimeException("Unsupported datetime unit. datetimteUnit = " + datetimteUnit);
            }
        } else if (t.equals(arrayType)) {
            PyArrayObject aryObj = new PyArrayObject(obj);
            int ndim = PyArray_NDIM(aryObj);

            SizeTPointer shapePtr = PyArray_DIMS(aryObj);
            long[] shape = new long[ndim];
            shapePtr.get(shape);

            SizeTPointer stridesPtr = PyArray_STRIDES(aryObj);
            long[] strides = new long[ndim];
            stridesPtr.get(strides);

            switch ((int) aryObj.descr().type()) {
                case NPY_BOOLLTR: {
                    BooleanPointer dataPtr = new BooleanPointer(PyArray_BYTES(aryObj));
                    boolean[] data = new boolean[lengthToInt(PyArray_Size(aryObj))];
                    dataPtr.get(data);
                    return new NpNdarrayBoolean(data, toIntArray(shape), toIntArray(strides));
                }
                case NPY_BYTELTR: {
                    BytePointer dataPtr = new BytePointer(PyArray_BYTES(aryObj));
                    byte[] data = new byte[lengthToInt(PyArray_Size(aryObj))];
                    dataPtr.get(data);
                    return new NpNdarrayByte(data, toIntArray(shape), toIntArray(strides));
                }
                case NPY_USHORTLTR: {
                    CharPointer dataPtr = new CharPointer(PyArray_BYTES(aryObj));
                    char[] data = new char[lengthToInt(PyArray_Size(aryObj))];
                    dataPtr.get(data);
                    return new NpNdarrayChar(data, toIntArray(shape), toIntArrayDiv(strides, 2));
                }
                case NPY_SHORTLTR: {
                    ShortPointer dataPtr = new ShortPointer(PyArray_BYTES(aryObj));
                    short[] data = new short[lengthToInt(PyArray_Size(aryObj))];
                    dataPtr.get(data);
                    return new NpNdarrayShort(data, toIntArray(shape), toIntArrayDiv(strides, 2));
                }
                case NPY_INTLTR: {
                    IntPointer dataPtr = new IntPointer(PyArray_BYTES(aryObj));
                    int[] data = new int[lengthToInt(PyArray_Size(aryObj))];
                    dataPtr.get(data);
                    return new NpNdarrayInt(data, toIntArray(shape), toIntArrayDiv(strides, 4));
                }
                case NPY_LONGLTR: {
                    int itemsize = (int) PyArray_ITEMSIZE(aryObj);
                    if (itemsize == 4) {
                        IntPointer dataPtr = new IntPointer(PyArray_BYTES(aryObj));
                        int[] data = new int[lengthToInt(PyArray_Size(aryObj))];
                        dataPtr.get(data);
                        return new NpNdarrayInt(data, toIntArray(shape), toIntArrayDiv(strides, 4));
                    } else if (itemsize == 8) {
                        LongPointer dataPtr = new LongPointer(PyArray_BYTES(aryObj));
                        long[] data = new long[lengthToInt(PyArray_Size(aryObj))];
                        dataPtr.get(data);
                        return new NpNdarrayLong(data, toIntArray(shape), toIntArrayDiv(strides, 8));
                    } else {
                        throw new RuntimeException("Unsupported itemsize for long. itemsize = " + itemsize);
                    }
                }
                case NPY_LONGLONGLTR: {
                    LongPointer dataPtr = new LongPointer(PyArray_BYTES(aryObj));
                    long[] data = new long[lengthToInt(PyArray_Size(aryObj))];
                    dataPtr.get(data);
                    return new NpNdarrayLong(data, toIntArray(shape), toIntArrayDiv(strides, 8));
                }
                case NPY_FLOATLTR: {
                    FloatPointer dataPtr = new FloatPointer(PyArray_BYTES(aryObj));
                    float[] data = new float[lengthToInt(PyArray_Size(aryObj))];
                    dataPtr.get(data);
                    return new NpNdarrayFloat(data, toIntArray(shape), toIntArrayDiv(strides, 4));
                }
                case NPY_DOUBLELTR: {
                    DoublePointer dataPtr = new DoublePointer(PyArray_BYTES(aryObj));
                    double[] data = new double[lengthToInt(PyArray_Size(aryObj))];
                    dataPtr.get(data);
                    return new NpNdarrayDouble(data, toIntArray(shape), toIntArrayDiv(strides, 8));
                }
                case NPY_DATETIMELTR: {
                    LongPointer dataPtr = new LongPointer(PyArray_BYTES(aryObj));
                    long[] longAry = new long[lengthToInt(PyArray_Size(aryObj))];
                    dataPtr.get(longAry);
                    Instant[] data = new Instant[longAry.length];

                    int datetimteUnit = new PyArray_DatetimeDTypeMetaData(aryObj.descr().c_metadata()).meta().base();
                    switch (datetimteUnit) {
                        case NPY_FR_W:
                            for (int i = 0; i < data.length; i++) {
                                data[i] = Instant.ofEpochSecond(longAry[i] * (7L * 24L * 60L * 60L));
                            }
                            break;
                        case NPY_FR_D:
                            for (int i = 0; i < data.length; i++) {
                                data[i] = Instant.ofEpochSecond(longAry[i] * (24L * 60L * 60L));
                            }
                            break;
                        case NPY_FR_h:
                            for (int i = 0; i < data.length; i++) {
                                data[i] = Instant.ofEpochSecond(longAry[i] * (60L * 60L));
                            }
                            break;
                        case NPY_FR_m:
                            for (int i = 0; i < data.length; i++) {
                                data[i] = Instant.ofEpochSecond(longAry[i] * 60L);
                            }
                            break;
                        case NPY_FR_s:
                            for (int i = 0; i < data.length; i++) {
                                data[i] = Instant.ofEpochSecond(longAry[i]);
                            }
                            break;
                        case NPY_FR_ms:
                            for (int i = 0; i < data.length; i++) {
                                data[i] = Instant.ofEpochMilli(longAry[i]);
                            }
                            break;
                        case NPY_FR_us:
                            for (int i = 0; i < data.length; i++) {
                                data[i] = Instant.ofEpochSecond(
                                        longAry[i] / (1000L * 1000L),
                                        longAry[i] % (1000L * 1000L));
                            }
                            break;
                        case NPY_FR_ns:
                            for (int i = 0; i < data.length; i++) {
                                data[i] = Instant.ofEpochSecond(
                                        longAry[i] / (1000L * 1000L * 1000L),
                                        longAry[i] % (1000L * 1000L * 1000L));
                            }
                            break;
                        default:
                            throw new RuntimeException("Unsupported datetime unit. datetimteUnit = " + datetimteUnit);
                    }
                    return new NpNdarrayInstant(data, toIntArray(shape), toIntArrayDiv(strides, 8));
                }
            }
        }
        throw new PythonException("Unsupported Python type");
    }

    private static int lengthToInt(long length) {
        if (length > Integer.MAX_VALUE) {
            throw new PythonException("Cannot convert because the length is larger than 2G");
        }
        return (int) length;
    }

    /**
     * Don't forget to call Py_DecRef().
     */
    private static PyObject toPyObject(Object value) {
        if (value == null) {
            return _Py_NoneStruct();
        } else if (value instanceof Boolean) {
            return PyBool_FromLong((Boolean) value ? 1 : 0);
        } else if (value instanceof Byte) {
            return PyLong_FromLong((Byte) value);
        } else if (value instanceof Character) {
            return PyLong_FromLong((Character) value);
        } else if (value instanceof Short) {
            return PyLong_FromLong((Short) value);
        } else if (value instanceof Integer) {
            return PyLong_FromLong((Integer) value);
        } else if (value instanceof Long) {
            return PyLong_FromLong((Long) value);
        } else if (value instanceof Float) {
            return PyFloat_FromDouble((Float) value);
        } else if (value instanceof Double) {
            return PyFloat_FromDouble((Double) value);
        } else if (value instanceof String) {
            return PyUnicode_FromString((String) value);
        } else if (value instanceof Instant) {
            Instant instant = (Instant) value;
            LongPointer ptr = new LongPointer(1).put(instant.toEpochMilli());
            PyArray_Descr descr = PyArray_DescrNewFromType(NPY_DATETIME);
            new PyArray_DatetimeDTypeMetaData(descr.c_metadata()).meta().base(NPY_FR_ms).num(1);
            return PyArray_Scalar(ptr, descr, null);
        } else if (value instanceof byte[]) {
            byte[] ary = (byte[]) value;
            return PyBytes_FromStringAndSize(new BytePointer(ary), ary.length);
        } else if (value instanceof boolean[]) {
            boolean[] ary = (boolean[]) value;
            SizeTPointer dims = new SizeTPointer(1).put(ary.length);
            BooleanPointer data = new BooleanPointer(ary);
            return PyArray_New(arrayType, 1, dims, NPY_BOOL, null, data, 0, NPY_ARRAY_CARRAY, null);
        } else if (value instanceof char[]) {
            char[] ary = (char[]) value;
            SizeTPointer dims = new SizeTPointer(1).put(ary.length);
            CharPointer data = new CharPointer(ary);
            return PyArray_New(arrayType, 1, dims, NPY_USHORT, null, data, 0, NPY_ARRAY_CARRAY, null);
        } else if (value instanceof short[]) {
            short[] ary = (short[]) value;
            SizeTPointer dims = new SizeTPointer(1).put(ary.length);
            ShortPointer data = new ShortPointer(ary);
            return PyArray_New(arrayType, 1, dims, NPY_SHORT, null, data, 0, NPY_ARRAY_CARRAY, null);
        } else if (value instanceof int[]) {
            int[] ary = (int[]) value;
            SizeTPointer dims = new SizeTPointer(1).put(ary.length);
            IntPointer data = new IntPointer(ary);
            return PyArray_New(arrayType, 1, dims, NPY_INT, null, data, 0, NPY_ARRAY_CARRAY, null);
        } else if (value instanceof long[]) {
            long[] ary = (long[]) value;
            SizeTPointer dims = new SizeTPointer(1).put(ary.length);
            LongPointer data = new LongPointer(ary);
            return PyArray_New(arrayType, 1, dims, NPY_LONGLONG, null, data, 0, NPY_ARRAY_CARRAY, null);
        } else if (value instanceof float[]) {
            float[] ary = (float[]) value;
            SizeTPointer dims = new SizeTPointer(1).put(ary.length);
            FloatPointer data = new FloatPointer(ary);
            return PyArray_New(arrayType, 1, dims, NPY_FLOAT, null, data, 0, NPY_ARRAY_CARRAY, null);
        } else if (value instanceof double[]) {
            double[] ary = (double[]) value;
            SizeTPointer dims = new SizeTPointer(1).put(ary.length);
            DoublePointer data = new DoublePointer(ary);
            return PyArray_New(arrayType, 1, dims, NPY_DOUBLE, null, data, 0, NPY_ARRAY_CARRAY, null);
        } else if (value instanceof Instant[]) {
            Instant[] ary = (Instant[]) value;
            SizeTPointer dims = new SizeTPointer(1).put(ary.length);
            LongPointer data = new LongPointer(Arrays.stream(ary).mapToLong(Instant::toEpochMilli).toArray());
            PyArray_Descr descr = PyArray_DescrNewFromType(NPY_DATETIME);
            new PyArray_DatetimeDTypeMetaData(descr.c_metadata()).meta().base(NPY_FR_ms).num(1);
            return PyArray_NewFromDescr(arrayType, descr, 1, dims, null, data, NPY_ARRAY_CARRAY, null);
        } else if (value instanceof NpNdarrayByte) {
            NpNdarrayByte ndary = (NpNdarrayByte) value;
            SizeTPointer dims = new SizeTPointer(toLongArray(ndary.shape));
            SizeTPointer strides = new SizeTPointer(ndary.stridesInBytes());
            BytePointer data = new BytePointer(ndary.data);
            return PyArray_New(arrayType, ndary.ndim(), dims, NPY_BYTE, strides, data, 0, NPY_ARRAY_CARRAY, null);
        } else if (value instanceof NpNdarrayBoolean) {
            NpNdarrayBoolean ndary = (NpNdarrayBoolean) value;
            SizeTPointer dims = new SizeTPointer(toLongArray(ndary.shape));
            SizeTPointer strides = new SizeTPointer(ndary.stridesInBytes());
            BooleanPointer data = new BooleanPointer(ndary.data);
            return PyArray_New(arrayType, ndary.ndim(), dims, NPY_BOOL, strides, data, 0, NPY_ARRAY_CARRAY, null);
        } else if (value instanceof NpNdarrayChar) {
            NpNdarrayChar ndary = (NpNdarrayChar) value;
            SizeTPointer dims = new SizeTPointer(toLongArray(ndary.shape));
            SizeTPointer strides = new SizeTPointer(ndary.stridesInBytes());
            CharPointer data = new CharPointer(ndary.data);
            return PyArray_New(arrayType, ndary.ndim(), dims, NPY_USHORT, strides, data, 0, NPY_ARRAY_CARRAY, null);
        } else if (value instanceof NpNdarrayShort) {
            NpNdarrayShort ndary = (NpNdarrayShort) value;
            SizeTPointer dims = new SizeTPointer(toLongArray(ndary.shape));
            SizeTPointer strides = new SizeTPointer(ndary.stridesInBytes());
            ShortPointer data = new ShortPointer(ndary.data);
            return PyArray_New(arrayType, ndary.ndim(), dims, NPY_SHORT, strides, data, 0, NPY_ARRAY_CARRAY, null);
        } else if (value instanceof NpNdarrayInt) {
            NpNdarrayInt ndary = (NpNdarrayInt) value;
            SizeTPointer dims = new SizeTPointer(toLongArray(ndary.shape));
            SizeTPointer strides = new SizeTPointer(ndary.stridesInBytes());
            IntPointer data = new IntPointer(ndary.data);
            return PyArray_New(arrayType, ndary.ndim(), dims, NPY_INT, strides, data, 0, NPY_ARRAY_CARRAY, null);
        } else if (value instanceof NpNdarrayLong) {
            NpNdarrayLong ndary = (NpNdarrayLong) value;
            SizeTPointer dims = new SizeTPointer(toLongArray(ndary.shape));
            SizeTPointer strides = new SizeTPointer(ndary.stridesInBytes());
            LongPointer data = new LongPointer(ndary.data);
            return PyArray_New(arrayType, ndary.ndim(), dims, NPY_LONGLONG, strides, data, 0, NPY_ARRAY_CARRAY, null);
        } else if (value instanceof NpNdarrayFloat) {
            NpNdarrayFloat ndary = (NpNdarrayFloat) value;
            SizeTPointer dims = new SizeTPointer(toLongArray(ndary.shape));
            SizeTPointer strides = new SizeTPointer(ndary.stridesInBytes());
            FloatPointer data = new FloatPointer(ndary.data);
            return PyArray_New(arrayType, ndary.ndim(), dims, NPY_FLOAT, strides, data, 0, NPY_ARRAY_CARRAY, null);
        } else if (value instanceof NpNdarrayDouble) {
            NpNdarrayDouble ndary = (NpNdarrayDouble) value;
            SizeTPointer dims = new SizeTPointer(toLongArray(ndary.shape));
            SizeTPointer strides = new SizeTPointer(ndary.stridesInBytes());
            DoublePointer data = new DoublePointer(ndary.data);
            return PyArray_New(arrayType, ndary.ndim(), dims, NPY_DOUBLE, strides, data, 0, NPY_ARRAY_CARRAY, null);
        } else if (value instanceof NpNdarrayInstant) {
            NpNdarrayInstant ndary = (NpNdarrayInstant) value;
            SizeTPointer dims = new SizeTPointer(toLongArray(ndary.shape));
            SizeTPointer strides = new SizeTPointer(ndary.stridesInBytes());
            LongPointer data = new LongPointer(Arrays.stream(ndary.data).mapToLong(Instant::toEpochMilli).toArray());
            PyArray_Descr descr = PyArray_DescrNewFromType(NPY_DATETIME);
            new PyArray_DatetimeDTypeMetaData(descr.c_metadata()).meta().base(NPY_FR_ms).num(1);
            return PyArray_NewFromDescr(arrayType, descr, ndary.ndim(), dims, strides, data, NPY_ARRAY_CARRAY, null);
        } else if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<Object, Object> map = (Map<Object, Object>) value;
            PyObject obj = PyDict_New();
            for (Object key : map.keySet()) {
                PyDict_SetItem(obj, toPyObject(key), toPyObject(map.get(key)));
            }
            return obj;
        } else if (value instanceof Object[]) {
            Object[] ary = (Object[]) value;
            PyObject obj = PyList_New(ary.length);
            for (int i = 0; i < ary.length; i++) {
                PyList_SetItem(obj, i, toPyObject(ary[i]));
            }
            return obj;
        } else if (value instanceof Iterable) {
            @SuppressWarnings("unchecked")
            Object[] ary = toObjectArray((Iterable<Object>) value);
            PyObject obj = PyList_New(ary.length);
            for (int i = 0; i < ary.length; i++) {
                PyList_SetItem(obj, i, toPyObject(ary[i]));
            }
            return obj;
        } else if (value instanceof scala.Function0) {
            @SuppressWarnings("unchecked")
            scala.Function0<Object> fn = (scala.Function0<Object>) value;
            return toPyCFunction(args -> fn.apply());
        } else if (value instanceof scala.Function1) {
            @SuppressWarnings("unchecked")
            scala.Function1<Object, Object> fn = (scala.Function1<Object, Object>) value;
            return toPyCFunction(args -> fn.apply(args[0]));
        } else if (value instanceof scala.Function2) {
            @SuppressWarnings("unchecked")
            scala.Function2<Object, Object, Object> fn = (scala.Function2<Object, Object, Object>) value;
            return toPyCFunction(args -> fn.apply(args[0], args[1]));
        } else if (value instanceof scala.Function3) {
            @SuppressWarnings("unchecked")
            scala.Function3<Object, Object, Object, Object> fn = (scala.Function3<Object, Object, Object, Object>) value;
            return toPyCFunction(args -> fn.apply(args[0], args[1], args[2]));
        } else if (value instanceof scala.Function4) {
            @SuppressWarnings("unchecked")
            scala.Function4<Object, Object, Object, Object, Object> fn = (scala.Function4<Object, Object, Object, Object, Object>) value;
            return toPyCFunction(args -> fn.apply(args[0], args[1], args[2], args[3]));
        } else if (value instanceof scala.Function5) {
            @SuppressWarnings("unchecked")
            scala.Function5<Object, Object, Object, Object, Object, Object> fn = (scala.Function5<Object, Object, Object, Object, Object, Object>) value;
            return toPyCFunction(args -> fn.apply(args[0], args[1], args[2], args[3], args[4]));
        } else if (value instanceof scala.Function6) {
            @SuppressWarnings("unchecked")
            scala.Function6<Object, Object, Object, Object, Object, Object, Object> fn = (scala.Function6<Object, Object, Object, Object, Object, Object, Object>) value;
            return toPyCFunction(args -> fn.apply(args[0], args[1], args[2], args[3], args[4], args[5]));
        } else if (value instanceof scala.Function7) {
            @SuppressWarnings("unchecked")
            scala.Function7<Object, Object, Object, Object, Object, Object, Object, Object> fn = (scala.Function7<Object, Object, Object, Object, Object, Object, Object, Object>) value;
            return toPyCFunction(args -> fn.apply(args[0], args[1], args[2], args[3], args[4], args[5], args[6]));
        } else if (value instanceof scala.Function8) {
            @SuppressWarnings("unchecked")
            scala.Function8<Object, Object, Object, Object, Object, Object, Object, Object, Object> fn = (scala.Function8<Object, Object, Object, Object, Object, Object, Object, Object, Object>) value;
            return toPyCFunction(args -> fn.apply(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7]));
        } else if (value instanceof scala.Function9) {
            @SuppressWarnings("unchecked")
            scala.Function9<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object> fn = (scala.Function9<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object>) value;
            return toPyCFunction(args -> fn.apply(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8]));
        } else if (value instanceof scala.Function10) {
            @SuppressWarnings("unchecked")
            scala.Function10<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object> fn = (scala.Function10<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object>) value;
            return toPyCFunction(args -> fn.apply(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9]));
        } else if (value instanceof scala.Function11) {
            @SuppressWarnings("unchecked")
            scala.Function11<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object> fn = (scala.Function11<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object>) value;
            return toPyCFunction(args -> fn.apply(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10]));
        } else if (value instanceof scala.Function12) {
            @SuppressWarnings("unchecked")
            scala.Function12<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object> fn = (scala.Function12<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object>) value;
            return toPyCFunction(args -> fn.apply(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11]));
        } else if (value instanceof scala.Function13) {
            @SuppressWarnings("unchecked")
            scala.Function13<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object> fn = (scala.Function13<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object>) value;
            return toPyCFunction(args -> fn.apply(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12]));
        } else if (value instanceof scala.Function14) {
            @SuppressWarnings("unchecked")
            scala.Function14<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object> fn = (scala.Function14<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object>) value;
            return toPyCFunction(args -> fn.apply(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13]));
        } else if (value instanceof scala.Function15) {
            @SuppressWarnings("unchecked")
            scala.Function15<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object> fn = (scala.Function15<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object>) value;
            return toPyCFunction(args -> fn.apply(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13], args[14]));
        } else if (value instanceof scala.Function16) {
            @SuppressWarnings("unchecked")
            scala.Function16<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object> fn = (scala.Function16<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object>) value;
            return toPyCFunction(args -> fn.apply(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13], args[14], args[15]));
        } else if (value instanceof scala.Function17) {
            @SuppressWarnings("unchecked")
            scala.Function17<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object> fn = (scala.Function17<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object>) value;
            return toPyCFunction(args -> fn.apply(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13], args[14], args[15], args[16]));
        } else if (value instanceof scala.Function18) {
            @SuppressWarnings("unchecked")
            scala.Function18<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object> fn = (scala.Function18<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object>) value;
            return toPyCFunction(args -> fn.apply(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13], args[14], args[15], args[16], args[17]));
        } else if (value instanceof scala.Function19) {
            @SuppressWarnings("unchecked")
            scala.Function19<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object> fn = (scala.Function19<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object>) value;
            return toPyCFunction(args -> fn.apply(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13], args[14], args[15], args[16], args[17], args[18]));
        } else if (value instanceof scala.Function20) {
            @SuppressWarnings("unchecked")
            scala.Function20<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object> fn = (scala.Function20<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object>) value;
            return toPyCFunction(args -> fn.apply(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13], args[14], args[15], args[16], args[17], args[18], args[19]));
        } else if (value instanceof scala.Function21) {
            @SuppressWarnings("unchecked")
            scala.Function21<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object> fn = (scala.Function21<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object>) value;
            return toPyCFunction(args -> fn.apply(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13], args[14], args[15], args[16], args[17], args[18], args[19], args[20]));
        } else if (value instanceof scala.Function22) {
            @SuppressWarnings("unchecked")
            scala.Function22<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object> fn = (scala.Function22<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object>) value;
            return toPyCFunction(args -> fn.apply(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13], args[14], args[15], args[16], args[17], args[18], args[19], args[20], args[21]));
        }
        throw new PythonException("Unsupported Java type. value = " + value);
    }

    private static PyObject toPyCFunction(Function<Object[], Object> fn) {
        PyCFunction pyFunc = new PyCFunction() {
            @Override
            public PyObject call(PyObject self, PyObject args) {
                try {
                    Object[] objs = new Object[(int) PyTuple_Size(args)];
                    for (int i = 0; i < objs.length; i++) {
                        objs[i] = toJava(PyTuple_GetItem(args, i));
                    }
                    return toPyObject(fn.apply(objs));
                } catch (Throwable e) {
                    e.printStackTrace();

                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw, true);
                    e.printStackTrace(pw);
                    String msg = sw.getBuffer().toString();
                    PyErr_SetString(PyExc_RuntimeError(), msg);

                    return null;
                }
            }
        };
        PyMethodDef methodDef = new PyMethodDef().ml_meth(pyFunc).ml_flags(METH_VARARGS);
        return PyCFunction_NewEx(methodDef, null, mainModule);
    }

    private static long[] toLongArray(int[] intAry) {
        return Arrays.stream(intAry).mapToLong(x -> x).toArray();
    }

    private static int[] toIntArray(long[] longAry) {
        return Arrays.stream(longAry).mapToInt(x -> (int) x).toArray();
    }

    private static int[] toIntArrayDiv(long[] longAry, int v) {
        return Arrays.stream(longAry).mapToInt(x -> (int) (x / v)).toArray();
    }

    private static Object[] toObjectArray(Iterable<Object> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false).toArray();
    }
}
