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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.bytedeco.cpython.global.python.*;
import static org.bytedeco.cpython.helper.python.Py_AddPath;
import static org.bytedeco.cpython.presets.python.cachePackages;
import static org.bytedeco.numpy.global.numpy.*;

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
                //noinspection unchecked
                return (T) toJava(obj);
            } finally {
                Py_DecRef(obj);
            }
        } finally {
            Py_DecRef(co);
        }
    }

    public synchronized static void exec(String src) {
        if (PyRun_SimpleStringFlags(src, null) != 0) {
            throw new PythonException("PyRun_SimpleStringFlags() failed. src = " + src);
        }
    }

    public synchronized static <T> T get(String name) {
        //noinspection unchecked
        return (T) toJava(getPyObject(name));
    }

    private static PyObject getPyObject(String name) {
        PyObject obj = PyDict_GetItemString(globals, name);
        if (obj == null) throw new NoSuchElementException("name = " + name);
        return obj;
    }

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
            byte[] ary = new byte[(int) PyBytes_Size(obj)];
            new BytePointer(PyBytes_AsString(obj)).get(ary);
            return ary;
        } else if (t.equals(byteArrayType)) {
            byte[] ary = new byte[(int) PyByteArray_Size(obj)];
            new BytePointer(PyByteArray_AsString(obj)).get(ary);
            return ary;
        } else if (t.equals(tupleType)) {
            Object[] ary = new Object[(int) PyTuple_Size(obj)];
            for (int i = 0; i < ary.length; i++) {
                ary[i] = toJava(PyTuple_GetItem(obj, i));
            }
            return ary;
        } else if (t.equals(listType)) {
            Object[] ary = new Object[(int) PyList_Size(obj)];
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
                map.put(toJava(key), toJava(value));
                if (ok != 0) break;
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
        } else if (t.equals(arrayType)) {
            PyArrayObject aryObj = new PyArrayObject(obj);
            int ndim = PyArray_NDIM(aryObj);

            SizeTPointer dimensionsPtr = PyArray_DIMS(aryObj);
            long[] dimensions = new long[ndim];
            dimensionsPtr.get(dimensions);

            SizeTPointer stridesPtr = PyArray_STRIDES(aryObj);
            long[] strides = new long[ndim];
            stridesPtr.get(strides);

            switch ((int) aryObj.descr().type()) {
                case NPY_BYTELTR: {
                    BytePointer dataPtr = new BytePointer(PyArray_BYTES(aryObj));
                    byte[] data = new byte[pyArraySize(aryObj)];
                    dataPtr.get(data);
                    return new NpNdarrayByte(data, toIntArray(dimensions), toIntArray(strides));
                }
                case NPY_USHORTLTR: {
                    CharPointer dataPtr = new CharPointer(PyArray_BYTES(aryObj));
                    char[] data = new char[pyArraySize(aryObj)];
                    dataPtr.get(data);
                    return new NpNdarrayChar(data, toIntArray(dimensions), toIntArrayDiv(strides, 2));
                }
                case NPY_SHORTLTR: {
                    ShortPointer dataPtr = new ShortPointer(PyArray_BYTES(aryObj));
                    short[] data = new short[pyArraySize(aryObj)];
                    dataPtr.get(data);
                    return new NpNdarrayShort(data, toIntArray(dimensions), toIntArrayDiv(strides, 2));
                }
                case NPY_INTLTR: {
                    IntPointer dataPtr = new IntPointer(PyArray_BYTES(aryObj));
                    int[] data = new int[pyArraySize(aryObj)];
                    dataPtr.get(data);
                    return new NpNdarrayInt(data, toIntArray(dimensions), toIntArrayDiv(strides, 4));
                }
                case NPY_LONGLTR: {
                    CLongPointer dataPtr = new CLongPointer(PyArray_BYTES(aryObj));
                    long[] data = new long[pyArraySize(aryObj)];
                    dataPtr.get(data);
                    return new NpNdarrayLong(data, toIntArray(dimensions), toIntArrayDiv(strides, 8));
                }
                case NPY_FLOATLTR: {
                    FloatPointer dataPtr = new FloatPointer(PyArray_BYTES(aryObj));
                    float[] data = new float[pyArraySize(aryObj)];
                    dataPtr.get(data);
                    return new NpNdarrayFloat(data, toIntArray(dimensions), toIntArrayDiv(strides, 4));
                }
                case NPY_DOUBLELTR: {
                    DoublePointer dataPtr = new DoublePointer(PyArray_BYTES(aryObj));
                    double[] data = new double[pyArraySize(aryObj)];
                    dataPtr.get(data);
                    return new NpNdarrayDouble(data, toIntArray(dimensions), toIntArrayDiv(strides, 8));
                }
            }
        }
        throw new PythonException("Unsupported Python type");
    }

    private static int pyArraySize(PyArrayObject aryObj) {
        long s = PyArray_Size(aryObj);
        if (s > Integer.MAX_VALUE) {
            throw new PythonException("Cannot convert np.ndarray because the length is larger than 2G");
        }
        return (int) s;
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
        } else if (value instanceof byte[]) {
            byte[] ary = (byte[]) value;
            return PyBytes_FromStringAndSize(new BytePointer(ary), ary.length);
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
            CLongPointer data = new CLongPointer(ary);
            return PyArray_New(arrayType, 1, dims, NPY_LONG, null, data, 0, NPY_ARRAY_CARRAY, null);
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
        } else if (value instanceof NpNdarrayByte) {
            NpNdarrayByte ndary = (NpNdarrayByte) value;
            SizeTPointer dims = new SizeTPointer(toLongArray(ndary.dimensions));
            SizeTPointer strides = new SizeTPointer(ndary.stridesInBytes());
            BytePointer data = new BytePointer(ndary.data);
            return PyArray_New(arrayType, ndary.ndim(), dims, NPY_BYTE, strides, data, 0, NPY_ARRAY_CARRAY, null);
        } else if (value instanceof NpNdarrayChar) {
            NpNdarrayChar ndary = (NpNdarrayChar) value;
            SizeTPointer dims = new SizeTPointer(toLongArray(ndary.dimensions));
            SizeTPointer strides = new SizeTPointer(ndary.stridesInBytes());
            CharPointer data = new CharPointer(ndary.data);
            return PyArray_New(arrayType, ndary.ndim(), dims, NPY_USHORT, strides, data, 0, NPY_ARRAY_CARRAY, null);
        } else if (value instanceof NpNdarrayShort) {
            NpNdarrayShort ndary = (NpNdarrayShort) value;
            SizeTPointer dims = new SizeTPointer(toLongArray(ndary.dimensions));
            SizeTPointer strides = new SizeTPointer(ndary.stridesInBytes());
            ShortPointer data = new ShortPointer(ndary.data);
            return PyArray_New(arrayType, ndary.ndim(), dims, NPY_SHORT, strides, data, 0, NPY_ARRAY_CARRAY, null);
        } else if (value instanceof NpNdarrayInt) {
            NpNdarrayInt ndary = (NpNdarrayInt) value;
            SizeTPointer dims = new SizeTPointer(toLongArray(ndary.dimensions));
            SizeTPointer strides = new SizeTPointer(ndary.stridesInBytes());
            IntPointer data = new IntPointer(ndary.data);
            return PyArray_New(arrayType, ndary.ndim(), dims, NPY_INT, strides, data, 0, NPY_ARRAY_CARRAY, null);
        } else if (value instanceof NpNdarrayLong) {
            NpNdarrayLong ndary = (NpNdarrayLong) value;
            SizeTPointer dims = new SizeTPointer(toLongArray(ndary.dimensions));
            SizeTPointer strides = new SizeTPointer(ndary.stridesInBytes());
            CLongPointer data = new CLongPointer(ndary.data);
            return PyArray_New(arrayType, ndary.ndim(), dims, NPY_LONG, strides, data, 0, NPY_ARRAY_CARRAY, null);
        } else if (value instanceof NpNdarrayFloat) {
            NpNdarrayFloat ndary = (NpNdarrayFloat) value;
            SizeTPointer dims = new SizeTPointer(toLongArray(ndary.dimensions));
            SizeTPointer strides = new SizeTPointer(ndary.stridesInBytes());
            FloatPointer data = new FloatPointer(ndary.data);
            return PyArray_New(arrayType, ndary.ndim(), dims, NPY_FLOAT, strides, data, 0, NPY_ARRAY_CARRAY, null);
        } else if (value instanceof NpNdarrayDouble) {
            NpNdarrayDouble ndary = (NpNdarrayDouble) value;
            SizeTPointer dims = new SizeTPointer(toLongArray(ndary.dimensions));
            SizeTPointer strides = new SizeTPointer(ndary.stridesInBytes());
            DoublePointer data = new DoublePointer(ndary.data);
            return PyArray_New(arrayType, ndary.ndim(), dims, NPY_DOUBLE, strides, data, 0, NPY_ARRAY_CARRAY, null);
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
        long[] longAry = new long[intAry.length];
        for (int i = 0; i < intAry.length; i++) {
            longAry[i] = intAry[i];
        }
        return longAry;
    }

    private static int[] toIntArray(long[] longAry) {
        int[] intAry = new int[longAry.length];
        for (int i = 0; i < longAry.length; i++) {
            intAry[i] = (int) longAry[i];
        }
        return intAry;
    }

    private static int[] toIntArrayDiv(long[] longAry, int v) {
        int[] intAry = new int[longAry.length];
        for (int i = 0; i < longAry.length; i++) {
            intAry[i] = (int) (longAry[i] / v);
        }
        return intAry;
    }

    private static Object[] toObjectArray(Iterable<Object> iterable) {
        ArrayList<Object> result = new ArrayList<>();
        for (Object obj : iterable) {
            result.add(obj);
        }
        return result.toArray();
    }
}
