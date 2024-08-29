package org.bytedeco.embeddedpython;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.bytedeco.cpython.global.python.METH_VARARGS;
import static org.bytedeco.cpython.global.python.PyBool_FromLong;
import static org.bytedeco.cpython.global.python.PyByteArray_AsString;
import static org.bytedeco.cpython.global.python.PyByteArray_Size;
import static org.bytedeco.cpython.global.python.PyBytes_AsString;
import static org.bytedeco.cpython.global.python.PyBytes_FromStringAndSize;
import static org.bytedeco.cpython.global.python.PyBytes_Size;
import static org.bytedeco.cpython.global.python.PyCFunction_NewEx;
import static org.bytedeco.cpython.global.python.PyConfig_Clear;
import static org.bytedeco.cpython.global.python.PyConfig_InitPythonConfig;
import static org.bytedeco.cpython.global.python.PyConfig_Read;
import static org.bytedeco.cpython.global.python.PyDict_GetItemString;
import static org.bytedeco.cpython.global.python.PyDict_New;
import static org.bytedeco.cpython.global.python.PyDict_Next;
import static org.bytedeco.cpython.global.python.PyDict_SetItem;
import static org.bytedeco.cpython.global.python.PyDict_SetItemString;
import static org.bytedeco.cpython.global.python.PyErr_Clear;
import static org.bytedeco.cpython.global.python.PyErr_Occurred;
import static org.bytedeco.cpython.global.python.PyErr_Print;
import static org.bytedeco.cpython.global.python.PyErr_SetString;
import static org.bytedeco.cpython.global.python.PyEval_EvalCode;
import static org.bytedeco.cpython.global.python.PyExc_RuntimeError;
import static org.bytedeco.cpython.global.python.PyFloat_AsDouble;
import static org.bytedeco.cpython.global.python.PyFloat_FromDouble;
import static org.bytedeco.cpython.global.python.PyImport_AddModule;
import static org.bytedeco.cpython.global.python.PyIter_Next;
import static org.bytedeco.cpython.global.python.PyList_Append;
import static org.bytedeco.cpython.global.python.PyList_New;
import static org.bytedeco.cpython.global.python.PyList_SetItem;
import static org.bytedeco.cpython.global.python.PyLong_AsLong;
import static org.bytedeco.cpython.global.python.PyLong_FromLong;
import static org.bytedeco.cpython.global.python.PyModule_GetDict;
import static org.bytedeco.cpython.global.python.PyObject_GetIter;
import static org.bytedeco.cpython.global.python.PyObject_Str;
import static org.bytedeco.cpython.global.python.PyRun_SimpleStringFlags;
import static org.bytedeco.cpython.global.python.PyTuple_GetItem;
import static org.bytedeco.cpython.global.python.PyTuple_Size;
import static org.bytedeco.cpython.global.python.PyUnicode_AsUTF8;
import static org.bytedeco.cpython.global.python.PyUnicode_FromString;
import static org.bytedeco.cpython.global.python.PyWideStringList_Append;
import static org.bytedeco.cpython.global.python.Py_CompileString;
import static org.bytedeco.cpython.global.python.Py_DecRef;
import static org.bytedeco.cpython.global.python.Py_DecodeLocale;
import static org.bytedeco.cpython.global.python.Py_InitializeFromConfig;
import static org.bytedeco.cpython.global.python.Py_eval_input;
import static org.bytedeco.cpython.global.python._Py_NoneStruct;
import static org.bytedeco.cpython.presets.python.cachePackages;
import static org.bytedeco.embeddedpython.PyTypes.PyBool_Check;
import static org.bytedeco.embeddedpython.PyTypes.PyByteArray_Check;
import static org.bytedeco.embeddedpython.PyTypes.PyBytes_Check;
import static org.bytedeco.embeddedpython.PyTypes.PyDict_Check;
import static org.bytedeco.embeddedpython.PyTypes.PyFloat_Check;
import static org.bytedeco.embeddedpython.PyTypes.PyLong_Check;
import static org.bytedeco.embeddedpython.PyTypes.PyNone_Check;
import static org.bytedeco.embeddedpython.PyTypes.PyUnicode_Check;
import static org.bytedeco.embeddedpython.PyTypes.arrayType;
import static org.bytedeco.embeddedpython.PyTypes.boolArrType;
import static org.bytedeco.embeddedpython.PyTypes.byteArrType;
import static org.bytedeco.embeddedpython.PyTypes.datetimeArrType;
import static org.bytedeco.embeddedpython.PyTypes.doubleArrType;
import static org.bytedeco.embeddedpython.PyTypes.floatArrType;
import static org.bytedeco.embeddedpython.PyTypes.intArrType;
import static org.bytedeco.embeddedpython.PyTypes.longArrType;
import static org.bytedeco.embeddedpython.PyTypes.shortArrType;
import static org.bytedeco.embeddedpython.PyTypes.ushortArrType;
import static org.bytedeco.numpy.global.numpy.NPY_ARRAY_CARRAY;
import static org.bytedeco.numpy.global.numpy.NPY_BOOL;
import static org.bytedeco.numpy.global.numpy.NPY_BOOLLTR;
import static org.bytedeco.numpy.global.numpy.NPY_BYTE;
import static org.bytedeco.numpy.global.numpy.NPY_BYTELTR;
import static org.bytedeco.numpy.global.numpy.NPY_DATETIME;
import static org.bytedeco.numpy.global.numpy.NPY_DATETIMELTR;
import static org.bytedeco.numpy.global.numpy.NPY_DOUBLE;
import static org.bytedeco.numpy.global.numpy.NPY_DOUBLELTR;
import static org.bytedeco.numpy.global.numpy.NPY_FLOAT;
import static org.bytedeco.numpy.global.numpy.NPY_FLOATLTR;
import static org.bytedeco.numpy.global.numpy.NPY_FR_D;
import static org.bytedeco.numpy.global.numpy.NPY_FR_W;
import static org.bytedeco.numpy.global.numpy.NPY_FR_h;
import static org.bytedeco.numpy.global.numpy.NPY_FR_m;
import static org.bytedeco.numpy.global.numpy.NPY_FR_ms;
import static org.bytedeco.numpy.global.numpy.NPY_FR_ns;
import static org.bytedeco.numpy.global.numpy.NPY_FR_s;
import static org.bytedeco.numpy.global.numpy.NPY_FR_us;
import static org.bytedeco.numpy.global.numpy.NPY_INT;
import static org.bytedeco.numpy.global.numpy.NPY_INTLTR;
import static org.bytedeco.numpy.global.numpy.NPY_LONGLONG;
import static org.bytedeco.numpy.global.numpy.NPY_LONGLONGLTR;
import static org.bytedeco.numpy.global.numpy.NPY_LONGLTR;
import static org.bytedeco.numpy.global.numpy.NPY_SHORT;
import static org.bytedeco.numpy.global.numpy.NPY_SHORTLTR;
import static org.bytedeco.numpy.global.numpy.NPY_USHORT;
import static org.bytedeco.numpy.global.numpy.NPY_USHORTLTR;
import static org.bytedeco.numpy.global.numpy.PyArray_BYTES;
import static org.bytedeco.numpy.global.numpy.PyArray_DIMS;
import static org.bytedeco.numpy.global.numpy.PyArray_DescrNewFromType;
import static org.bytedeco.numpy.global.numpy.PyArray_ITEMSIZE;
import static org.bytedeco.numpy.global.numpy.PyArray_NDIM;
import static org.bytedeco.numpy.global.numpy.PyArray_New;
import static org.bytedeco.numpy.global.numpy.PyArray_NewFromDescr;
import static org.bytedeco.numpy.global.numpy.PyArray_STRIDES;
import static org.bytedeco.numpy.global.numpy.PyArray_Scalar;
import static org.bytedeco.numpy.global.numpy.PyArray_Size;
import static org.bytedeco.numpy.global.numpy._import_array;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;

import org.bytedeco.cpython.PyCFunction;
import org.bytedeco.cpython.PyConfig;
import org.bytedeco.cpython.PyMethodDef;
import org.bytedeco.cpython.PyObject;
import org.bytedeco.cpython.PyStatus;
import org.bytedeco.cpython.PyTypeObject;
import org.bytedeco.cpython.PyWideStringList;
import org.bytedeco.cpython.global.python;
import org.bytedeco.javacpp.BooleanPointer;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.CharPointer;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.LongPointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerScope;
import org.bytedeco.javacpp.ShortPointer;
import org.bytedeco.javacpp.SizeTPointer;
import org.bytedeco.numpy.PyArrayObject;
import org.bytedeco.numpy.PyArray_DatetimeDTypeMetaData;
import org.bytedeco.numpy.PyArray_Descr;
import org.bytedeco.numpy.PyBoolScalarObject;
import org.bytedeco.numpy.PyByteScalarObject;
import org.bytedeco.numpy.PyDatetimeScalarObject;
import org.bytedeco.numpy.PyDoubleScalarObject;
import org.bytedeco.numpy.PyFloatScalarObject;
import org.bytedeco.numpy.PyIntScalarObject;
import org.bytedeco.numpy.PyLongScalarObject;
import org.bytedeco.numpy.PyShortScalarObject;
import org.bytedeco.numpy.PyUShortScalarObject;

/**
 * Python interpreter.
 * <p>
 * If you embed two Python interpreters, many Python libraries do not work correctly.
 * Therefore this class is a singleton class. All the methods are static.
 * <p>
 * This class is thread-safe. All the methods are synchronized.
 */
public class Python {
    private static final PyObject MAIN_MODULE;
    private static final PyObject GLOBALS;
    static {
        try {
            init();
        } catch (Exception e) {
            throw new PythonException("Failed at Python.init()", e);
        }
        MAIN_MODULE = PyImport_AddModule("__main__");
        if (MAIN_MODULE == null) { // don't kill the entire JVM
            throw new PythonException("Failed to load __main__");
        }
        GLOBALS = PyModule_GetDict(MAIN_MODULE);
    }

    private static void init() throws IOException {
        System.setProperty("org.bytedeco.openblas.load", "mkl");

        List<File> moduleSearchPaths = new ArrayList<>();

        Collections.addAll(moduleSearchPaths, cachePackages());
        Collections.addAll(moduleSearchPaths, org.bytedeco.numpy.presets.numpy.cachePackages());

        String pythonPath = Loader.load(org.bytedeco.cpython.python.class);

        try (PyConfig config = new PyConfig(); //
                PointerScope scope = new PointerScope()) {
            // See
            // https://docs.python.org/3.12/c-api/init_config.html#c.Py_InitializeFromConfig
            // and https://docs.python.org/3.12/c-api/init.html#c.Py_SetPath

            PyConfig_InitPythonConfig(config);
            requireNoException(PyConfig_Read(config));

            PyWideStringList modulePaths = config.module_search_paths();
            for (File packageLocation : moduleSearchPaths) {
                Pointer path = toWcharTPointer(packageLocation.getPath());
                requireNoException(PyWideStringList_Append(modulePaths, path));
            }
            config.module_search_paths_set(1);

            PyWideStringList argv = config.argv();
            Pointer emptyString = toWcharTPointer("");
            requireNoException(PyWideStringList_Append(argv, emptyString));

            config.executable(toWcharTPointer(pythonPath));
            config.buffered_stdio(1);

            requireNoException(Py_InitializeFromConfig(config));
            PyConfig_Clear(config);
        }
        _import_array();

        Runtime.getRuntime().addShutdownHook(new Thread(python::Py_Finalize));
    }

    private static Pointer toWcharTPointer(String string) {
        Pointer decodedString = Py_DecodeLocale(string, null);
        if (!Pointer.isNull(decodedString)) {
            PointerScope.getInnerScope().attach(decodedString);
        }
        return decodedString;
    }

    private static void requireNoException(PyStatus status) {
        if (python.PyStatus_Exception(status) != 0) {
            throw new IllegalStateException();
        }
    }

    private Python() {
    }

    /**
     * Don't forget to call Py_DecRef().
     */
    private static PyObject compile(String src) {
        PyObject co = Py_CompileString(src, "<string>", Py_eval_input);
        if (co == null) {
            if (PyErr_Occurred() != null) {
                PyErr_Print();
            }
            throw new PythonException("Py_CompileString() failed. src = " + src);
        }
        return co;
    }

    /**
     * Python built-in eval().
     *
     * @param src Python code. This must be a single line code.
     * @param <T> The Java class after conversion from Python.
     * @return The Java object converted from the Python object.
     */
    @SuppressWarnings("unchecked")
    public synchronized static <T> T eval(String src) {
        PyObject co = compile(src);
        try {
            PyObject obj = PyEval_EvalCode(co, GLOBALS, GLOBALS);
            try {
                if (obj == null) {
                    if (PyErr_Occurred() != null) {
                        PyErr_Print();
                        throw new PythonException("PyEval_EvalCode() failed. An Error is thrown inside Python. src = " + src);
                    } else {
                        throw new PythonException("PyEval_EvalCode() failed. src = " + src);
                    }
                }
                TypeTreeBuilder builder = new TypeTreeBuilder(1);
                return (T) toJava(obj, builder);
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
     *
     * <table border="1">
     * <caption>Type mappings. Python to Java.</caption>
     * <thead><tr><th>Python</th><th>Java</th></tr></thead>
     * <tbody>
     * <tr><td>None</td><td>null</td></tr>
     * <tr><td>bool</td><td>boolean</td></tr>
     * <tr><td>int</td><td>long</td></tr>
     * <tr><td>float</td><td>double</td></tr>
     * <tr><td>str</td><td>String</td></tr>
     * <tr><td>scalar np.bool8</td><td>boolean</td></tr>
     * <tr><td>scalar np.int8</td><td>byte</td></tr>
     * <tr><td>scalar np.int16</td><td>short</td></tr>
     * <tr><td>scalar np.uint16</td><td>char</td></tr>
     * <tr><td>scalar np.int32</td><td>int</td></tr>
     * <tr><td>scalar np.int64</td><td>long</td></tr>
     * <tr><td>scalar np.float32</td><td>float</td></tr>
     * <tr><td>scalar np.float64</td><td>double</td></tr>
     * <tr><td>scalar np.datetime64[W, D, h, m, s, ms, us, or ns]</td><td>Instant</td></tr>
     * <tr><td>bytes</td><td>byte[]</td></tr>
     * <tr><td>bytearray</td><td>byte[]</td></tr>
     * <tr><td>dict</td><td>LinkedHashMap</td></tr>
     * <tr><td>ndarray np.bool8</td><td>NpNdarrayBoolean</td></tr>
     * <tr><td>ndarray np.int8</td><td>NpNdarrayByte</td></tr>
     * <tr><td>ndarray np.int16</td><td>NpNdarrayShort</td></tr>
     * <tr><td>ndarray np.uint16</td><td>NpNdarrayChar</td></tr>
     * <tr><td>ndarray np.int32</td><td>NpNdarrayInt</td></tr>
     * <tr><td>ndarray np.int64</td><td>NpNdarrayLong</td></tr>
     * <tr><td>ndarray np.float32</td><td>NpNdarrayFloat</td></tr>
     * <tr><td>ndarray np.float64</td><td>NpNdarrayDouble</td></tr>
     * <tr><td>ndarray np.datetime64[W, D, h, m, s, ms, us, or ns]</td><td>NpNdarrayInstant</td></tr>
     * <tr><td>iterable</td><td>ArrayList</td></tr>
     * </tbody>
     * </table>
     *
     * @param name The variable name
     * @param <T>  The Java class after conversion from Python.
     * @return The Java object converted from the Python object.
     * @throws PythonException        If the value cannot convert to a Java object.
     * @throws NoSuchElementException If the variable does not exists.
     */
    @SuppressWarnings("unchecked")
    public synchronized static <T> T get(String name) {
        TypeTreeBuilder builder = new TypeTreeBuilder(1);
        return (T) toJava(getPyObject(name), builder);
    }

    private static PyObject getPyObject(String name) {
        PyObject obj = PyDict_GetItemString(GLOBALS, name);
        if (obj == null) throw new NoSuchElementException("name = " + name);
        return obj;
    }

    /**
     * Convert the Java object and set it to the global Python variable.
     *
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
     * <tr><td>Instant</td><td>np.datetime64[ns]</td></tr>
     * <tr><td>String</td><td>str</td></tr>
     * <tr><td>byte[]</td><td>bytes</td></tr>
     * <tr><td>boolean[]</td><td>np.ndarray, dtype=np.bool8</td></tr>
     * <tr><td>short[]</td><td>np.ndarray, dtype=np.int16</td></tr>
     * <tr><td>char[]</td><td>np.ndarray, dtype=np.uint16</td></tr>
     * <tr><td>int[]</td><td>np.ndarray, dtype=np.int32</td></tr>
     * <tr><td>long[]</td><td>np.ndarray, dtype=np.int64</td></tr>
     * <tr><td>float[]</td><td>np.ndarray, dtype=np.float32</td></tr>
     * <tr><td>double[]</td><td>np.ndarray, dtype=np.float64</td></tr>
     * <tr><td>Instant[]</td><td>np.ndarray, dtype=np.datetime64[ns]</td></tr>
     * <tr><td>NpNdarrayBoolean</td><td>np.ndarray, dtype=np.bool8</td></tr>
     * <tr><td>NpNdarrayByte</td><td>np.ndarray, dtype=np.int8</td></tr>
     * <tr><td>NpNdarrayShort</td><td>np.ndarray, dtype=np.int16</td></tr>
     * <tr><td>NpNdarrayChar</td><td>np.ndarray, dtype=np.uint16</td></tr>
     * <tr><td>NpNdarrayInt</td><td>np.ndarray, dtype=np.int32</td></tr>
     * <tr><td>NpNdarrayLong</td><td>np.ndarray, dtype=np.int64</td></tr>
     * <tr><td>NpNdarrayFloat</td><td>np.ndarray, dtype=np.float32</td></tr>
     * <tr><td>NpNdarrayDouble</td><td>np.ndarray, dtype=np.float64</td></tr>
     * <tr><td>NpNdarrayInstant</td><td>np.ndarray, dtype=np.datetime64[ns]</td></tr>
     * <tr><td>java.util.Map</td><td>dict</td></tr>
     * <tr><td>scala.collection.Map</td><td>dict</td></tr>
     * <tr><td>Object[]</td><td>list</td></tr>
     * <tr><td>Iterable</td><td>list</td></tr>
     * <tr><td>scala.Function0 - Function22</td><td>built-in global Python function</td></tr>
     * </tbody>
     * </table>
     *
     * @param name  The variable name
     * @param value The value to put.
     * @throws PythonException If the value cannot convert to a Python object.
     */
    public synchronized static void put(String name, Object value) {
        TypeTreeBuilder builder = new TypeTreeBuilder(1);
        putPyObject(name, toPyObject(value, builder));
    }

    private static void putPyObject(String name, PyObject obj) {
        try {
            if (PyDict_SetItemString(GLOBALS, name, obj) != 0) {
                throw new PythonException("PyDict_SetItemString() failed");
            }
        } finally {
            Py_DecRef(obj);
        }
    }

    private static Object toJava(PyObject obj, TypeTreeBuilder builder) {
        PyObject iterator;
        PyTypeObject t = PyTypes.Py_TYPE(obj);
        if (PyNone_Check(obj)) {
            builder.addType("None");
            return null;
        } else if (PyBool_Check(obj)) {
            builder.addType("bool");
            return PyLong_AsLong(obj) != 0;
        } else if (PyLong_Check(obj)) {
            builder.addType("int");
            return PyLong_AsLong(obj);
        } else if (PyFloat_Check(obj)) {
            builder.addType("float");
            return PyFloat_AsDouble(obj);
        } else if (PyUnicode_Check(obj)) {
            builder.addType("str");
            return new BytePointer(PyUnicode_AsUTF8(obj)).getString(UTF_8);
        } else if (t.equals(boolArrType)) {
            builder.addType("np.bool8");
            return new PyBoolScalarObject(obj).obval() != 0;
        } else if (t.equals(byteArrType)) {
            builder.addType("np.int8");
            return new PyByteScalarObject(obj).obval();
        } else if (t.equals(ushortArrType)) {
            builder.addType("np.uint8");
            return (char) (new PyUShortScalarObject(obj).obval());
        } else if (t.equals(shortArrType)) {
            builder.addType("np.int16");
            return new PyShortScalarObject(obj).obval();
        } else if (t.equals(intArrType)) {
            builder.addType("np.int32");
            return new PyIntScalarObject(obj).obval();
        } else if (t.equals(longArrType)) {
            builder.addType("np.int64");
            return new PyLongScalarObject(obj).obval();
        } else if (t.equals(floatArrType)) {
            builder.addType("np.float32");
            return new PyFloatScalarObject(obj).obval();
        } else if (t.equals(doubleArrType)) {
            builder.addType("np.float64");
            return new PyDoubleScalarObject(obj).obval();
        } else if (t.equals(datetimeArrType)) {
            PyDatetimeScalarObject datetimeScalarObj = new PyDatetimeScalarObject(obj);
            int datetimteUnit = datetimeScalarObj.obmeta().base();
            switch (datetimteUnit) {
                case NPY_FR_W:
                    builder.addType("np.datetime64[W]");
                    return Instant.ofEpochSecond(datetimeScalarObj.obval() * (7L * 24L * 60L * 60L));
                case NPY_FR_D:
                    builder.addType("np.datetime64[D]");
                    return Instant.ofEpochSecond(datetimeScalarObj.obval() * (24L * 60L * 60L));
                case NPY_FR_h:
                    builder.addType("np.datetime64[h]");
                    return Instant.ofEpochSecond(datetimeScalarObj.obval() * (60L * 60L));
                case NPY_FR_m:
                    builder.addType("np.datetime64[m]");
                    return Instant.ofEpochSecond(datetimeScalarObj.obval() * 60L);
                case NPY_FR_s:
                    builder.addType("np.datetime64[s]");
                    return Instant.ofEpochSecond(datetimeScalarObj.obval());
                case NPY_FR_ms:
                    builder.addType("np.datetime64[ms]");
                    return Instant.ofEpochMilli(datetimeScalarObj.obval());
                case NPY_FR_us:
                    builder.addType("np.datetime64[us]");
                    return Instant.ofEpochSecond(
                            datetimeScalarObj.obval() / 1000_000L,
                            datetimeScalarObj.obval() % 1000_000L);
                case NPY_FR_ns:
                    builder.addType("np.datetime64[ns]");
                    return Instant.ofEpochSecond(
                            datetimeScalarObj.obval() / 1000_000_000L,
                            datetimeScalarObj.obval() % 1000_000_000L);
                default:
                    builder.addType("np.datetime64[???]  <- Unsupported datetime unit " + datetimteUnit);
                    throw new PythonException("Cannot convert the Python object to a Java object.\n" +
                            "\nValue type tree\n" + builder.toString());
            }
        } else if (PyBytes_Check(obj)) {
            builder.addType("bytes");
            byte[] ary = new byte[lengthToInt(PyBytes_Size(obj))];
            new BytePointer(PyBytes_AsString(obj)).get(ary);
            return ary;
        } else if (PyByteArray_Check(obj)) {
            builder.addType("bytearray");
            byte[] ary = new byte[lengthToInt(PyByteArray_Size(obj))];
            new BytePointer(PyByteArray_AsString(obj)).get(ary);
            return ary;
        } else if (PyDict_Check(obj)) {
            builder.addType("dict");
            builder.tab++;

            SizeTPointer pos = new SizeTPointer(1).put(0);
            LinkedHashMap<Object, Object> map = new LinkedHashMap<>();
            while (true) {
                PyObject key = new PyObject();
                PyObject value = new PyObject();
                int ok = PyDict_Next(obj, pos, key, value);
                if (ok == 0) break;

                builder.addType("item");
                builder.tab++;
                map.put(toJava(key, builder), toJava(value, builder));
                builder.tab--;
            }

            builder.tab--;
            return map;
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
                    builder.addType("np.ndarray(dtype=np.bool8)");
                    BooleanPointer dataPtr = new BooleanPointer(PyArray_BYTES(aryObj));
                    boolean[] data = new boolean[lengthToInt(PyArray_Size(aryObj))];
                    dataPtr.get(data);
                    return new NpNdarrayBoolean(data, toIntArray(shape), toIntArray(strides));
                }
                case NPY_BYTELTR: {
                    builder.addType("np.ndarray(dtype=np.int8)");
                    BytePointer dataPtr = new BytePointer(PyArray_BYTES(aryObj));
                    byte[] data = new byte[lengthToInt(PyArray_Size(aryObj))];
                    dataPtr.get(data);
                    return new NpNdarrayByte(data, toIntArray(shape), toIntArray(strides));
                }
                case NPY_USHORTLTR: {
                    builder.addType("np.ndarray(dtype=np.uint16)");
                    CharPointer dataPtr = new CharPointer(PyArray_BYTES(aryObj));
                    char[] data = new char[lengthToInt(PyArray_Size(aryObj))];
                    dataPtr.get(data);
                    return new NpNdarrayChar(data, toIntArray(shape), toIntArrayDiv(strides, 2));
                }
                case NPY_SHORTLTR: {
                    builder.addType("np.ndarray(dtype=np.int16)");
                    ShortPointer dataPtr = new ShortPointer(PyArray_BYTES(aryObj));
                    short[] data = new short[lengthToInt(PyArray_Size(aryObj))];
                    dataPtr.get(data);
                    return new NpNdarrayShort(data, toIntArray(shape), toIntArrayDiv(strides, 2));
                }
                case NPY_INTLTR: {
                    builder.addType("np.ndarray(dtype=np.int32)");
                    IntPointer dataPtr = new IntPointer(PyArray_BYTES(aryObj));
                    int[] data = new int[lengthToInt(PyArray_Size(aryObj))];
                    dataPtr.get(data);
                    return new NpNdarrayInt(data, toIntArray(shape), toIntArrayDiv(strides, 4));
                }
                case NPY_LONGLTR: {
                    int itemsize = (int) PyArray_ITEMSIZE(aryObj);
                    if (itemsize == 4) {
                        builder.addType("np.ndarray(dtype=np.int32)");
                        IntPointer dataPtr = new IntPointer(PyArray_BYTES(aryObj));
                        int[] data = new int[lengthToInt(PyArray_Size(aryObj))];
                        dataPtr.get(data);
                        return new NpNdarrayInt(data, toIntArray(shape), toIntArrayDiv(strides, 4));
                    } else if (itemsize == 8) {
                        builder.addType("np.ndarray(dtype=np.int64)");
                        LongPointer dataPtr = new LongPointer(PyArray_BYTES(aryObj));
                        long[] data = new long[lengthToInt(PyArray_Size(aryObj))];
                        dataPtr.get(data);
                        return new NpNdarrayLong(data, toIntArray(shape), toIntArrayDiv(strides, 8));
                    } else {
                        builder.addType("np.ndarray(dtype=???)  <- Unsupported itemsize " + itemsize);
                        throw new PythonException("Cannot convert the Python object to a Java object.\n" +
                                "\nValue type tree\n" + builder.toString());
                    }
                }
                case NPY_LONGLONGLTR: {
                    builder.addType("np.ndarray(dtype=np.int64)");
                    LongPointer dataPtr = new LongPointer(PyArray_BYTES(aryObj));
                    long[] data = new long[lengthToInt(PyArray_Size(aryObj))];
                    dataPtr.get(data);
                    return new NpNdarrayLong(data, toIntArray(shape), toIntArrayDiv(strides, 8));
                }
                case NPY_FLOATLTR: {
                    builder.addType("np.ndarray(dtype=np.float32)");
                    FloatPointer dataPtr = new FloatPointer(PyArray_BYTES(aryObj));
                    float[] data = new float[lengthToInt(PyArray_Size(aryObj))];
                    dataPtr.get(data);
                    return new NpNdarrayFloat(data, toIntArray(shape), toIntArrayDiv(strides, 4));
                }
                case NPY_DOUBLELTR: {
                    builder.addType("np.ndarray(dtype=np.float64)");
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
                            builder.addType("np.ndarray(dtype=np.datetime64[W])");
                            for (int i = 0; i < data.length; i++) {
                                data[i] = Instant.ofEpochSecond(longAry[i] * (7L * 24L * 60L * 60L));
                            }
                            break;
                        case NPY_FR_D:
                            builder.addType("np.ndarray(dtype=np.datetime64[D])");
                            for (int i = 0; i < data.length; i++) {
                                data[i] = Instant.ofEpochSecond(longAry[i] * (24L * 60L * 60L));
                            }
                            break;
                        case NPY_FR_h:
                            builder.addType("np.ndarray(dtype=np.datetime64[h])");
                            for (int i = 0; i < data.length; i++) {
                                data[i] = Instant.ofEpochSecond(longAry[i] * (60L * 60L));
                            }
                            break;
                        case NPY_FR_m:
                            builder.addType("np.ndarray(dtype=np.datetime64[m])");
                            for (int i = 0; i < data.length; i++) {
                                data[i] = Instant.ofEpochSecond(longAry[i] * 60L);
                            }
                            break;
                        case NPY_FR_s:
                            builder.addType("np.ndarray(dtype=np.datetime64[s])");
                            for (int i = 0; i < data.length; i++) {
                                data[i] = Instant.ofEpochSecond(longAry[i]);
                            }
                            break;
                        case NPY_FR_ms:
                            builder.addType("np.ndarray(dtype=np.datetime64[ms])");
                            for (int i = 0; i < data.length; i++) {
                                data[i] = Instant.ofEpochMilli(longAry[i]);
                            }
                            break;
                        case NPY_FR_us:
                            builder.addType("np.ndarray(dtype=np.datetime64[us])");
                            for (int i = 0; i < data.length; i++) {
                                data[i] = Instant.ofEpochSecond(
                                        longAry[i] / 1000_000L,
                                        longAry[i] % 1000_000L);
                            }
                            break;
                        case NPY_FR_ns:
                            builder.addType("np.ndarray(dtype=np.datetime64[ns])");
                            for (int i = 0; i < data.length; i++) {
                                data[i] = Instant.ofEpochSecond(
                                        longAry[i] / 1000_000_000L,
                                        longAry[i] % 1000_000_000L);
                            }
                            break;
                        default:
                            builder.addType("np.ndarray(dtype=np.datetime64[???])  <- Unsupported datetime unit " + datetimteUnit);
                            throw new PythonException("Cannot convert the Python object to a Java object.\n" +
                                    "\nValue type tree\n" + builder.toString());
                    }
                    return new NpNdarrayInstant(data, toIntArray(shape), toIntArrayDiv(strides, 8));
                }
                // default:
                // goto iterable type
            }
        } else if ((iterator = getIter(obj)) != null) {
            try {
                builder.addType("iterable(" + new BytePointer(t.tp_name()).getString(UTF_8) + ")");
                builder.tab++;

                ArrayList<Object> list = new ArrayList<>();
                while (true) {
                    PyObject item = PyIter_Next(iterator);
                    try {
                        if (item == null) break;
                        list.add(toJava(item, builder));
                    } finally {
                        Py_DecRef(item);
                    }
                }

                builder.tab--;
                return list;
            } finally {
                Py_DecRef(iterator);
            }
        }

        builder.addType(new BytePointer(t.tp_name()).getString(UTF_8) + "  <- Unsupported");
        PyObject valueStrObj = PyObject_Str(obj);
        try {
            String msgPrefix = "Cannot convert the Python object to a Java object.\n" +
                    "\nValue type tree\n" + builder.toString();
            if (valueStrObj == null) {
                throw new PythonException(msgPrefix);
            } else {
                String valueStr = new BytePointer(PyUnicode_AsUTF8(valueStrObj)).getString(UTF_8);
                throw new PythonException(msgPrefix + "\nvalue = " + valueStr);
            }
        } finally {
            Py_DecRef(valueStrObj);
        }
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
    private static PyObject toPyObject(Object value, TypeTreeBuilder builder) {
        if (value == null) {
            builder.addType("null");
            return _Py_NoneStruct();
        } else if (value instanceof Boolean) {
            builder.addType("Boolean");
            return PyBool_FromLong((Boolean) value ? 1 : 0);
        } else if (value instanceof Byte) {
            builder.addType("Byte");
            return PyLong_FromLong((Byte) value);
        } else if (value instanceof Character) {
            builder.addType("Character");
            return PyLong_FromLong((Character) value);
        } else if (value instanceof Short) {
            builder.addType("Short");
            return PyLong_FromLong((Short) value);
        } else if (value instanceof Integer) {
            builder.addType("Integer");
            return PyLong_FromLong((Integer) value);
        } else if (value instanceof Long) {
            builder.addType("Long");
            return PyLong_FromLong((Long) value);
        } else if (value instanceof Float) {
            builder.addType("Float");
            return PyFloat_FromDouble((Float) value);
        } else if (value instanceof Double) {
            builder.addType("Double");
            return PyFloat_FromDouble((Double) value);
        } else if (value instanceof Instant) {
            builder.addType("Instant");
            try {
                Instant instant = (Instant) value;
                LongPointer ptr = new LongPointer(1).put(
                        Math.addExact(Math.multiplyExact(instant.getEpochSecond(), 1000_000_000L), instant.getNano()));
                PyArray_Descr descr = PyArray_DescrNewFromType(NPY_DATETIME);
                new PyArray_DatetimeDTypeMetaData(descr.c_metadata()).meta().base(NPY_FR_ns).num(1);
                return PyArray_Scalar(ptr, descr, null);
            } catch (ArithmeticException e) {
                throw new RuntimeException("Instant date range is outside of datetime64[ns] (1678-2262).", e);
            }
        } else if (value instanceof String) {
            builder.addType("String");
            return PyUnicode_FromString((String) value);
        } else if (value instanceof byte[]) {
            builder.addType("byte[]");
            byte[] ary = (byte[]) value;
            return PyBytes_FromStringAndSize(new BytePointer(ary), ary.length);
        } else if (value instanceof boolean[]) {
            builder.addType("boolean[]");
            boolean[] ary = (boolean[]) value;
            SizeTPointer dims = new SizeTPointer(1).put(ary.length);
            BooleanPointer data = new BooleanPointer(ary);
            return PyArray_New(arrayType, 1, dims, NPY_BOOL, null, data, 0, NPY_ARRAY_CARRAY, null);
        } else if (value instanceof char[]) {
            builder.addType("char[]");
            char[] ary = (char[]) value;
            SizeTPointer dims = new SizeTPointer(1).put(ary.length);
            CharPointer data = new CharPointer(ary);
            return PyArray_New(arrayType, 1, dims, NPY_USHORT, null, data, 0, NPY_ARRAY_CARRAY, null);
        } else if (value instanceof short[]) {
            builder.addType("short[]");
            short[] ary = (short[]) value;
            SizeTPointer dims = new SizeTPointer(1).put(ary.length);
            ShortPointer data = new ShortPointer(ary);
            return PyArray_New(arrayType, 1, dims, NPY_SHORT, null, data, 0, NPY_ARRAY_CARRAY, null);
        } else if (value instanceof int[]) {
            builder.addType("int[]");
            int[] ary = (int[]) value;
            SizeTPointer dims = new SizeTPointer(1).put(ary.length);
            IntPointer data = new IntPointer(ary);
            return PyArray_New(arrayType, 1, dims, NPY_INT, null, data, 0, NPY_ARRAY_CARRAY, null);
        } else if (value instanceof long[]) {
            builder.addType("long[]");
            long[] ary = (long[]) value;
            SizeTPointer dims = new SizeTPointer(1).put(ary.length);
            LongPointer data = new LongPointer(ary);
            return PyArray_New(arrayType, 1, dims, NPY_LONGLONG, null, data, 0, NPY_ARRAY_CARRAY, null);
        } else if (value instanceof float[]) {
            builder.addType("float[]");
            float[] ary = (float[]) value;
            SizeTPointer dims = new SizeTPointer(1).put(ary.length);
            FloatPointer data = new FloatPointer(ary);
            return PyArray_New(arrayType, 1, dims, NPY_FLOAT, null, data, 0, NPY_ARRAY_CARRAY, null);
        } else if (value instanceof double[]) {
            builder.addType("double[]");
            double[] ary = (double[]) value;
            SizeTPointer dims = new SizeTPointer(1).put(ary.length);
            DoublePointer data = new DoublePointer(ary);
            return PyArray_New(arrayType, 1, dims, NPY_DOUBLE, null, data, 0, NPY_ARRAY_CARRAY, null);
        } else if (value instanceof Instant[]) {
            builder.addType("Instant[]");
            try {
                Instant[] ary = (Instant[]) value;
                SizeTPointer dims = new SizeTPointer(1).put(ary.length);
                LongPointer data = new LongPointer(Arrays.stream(ary).mapToLong(instant ->
                        Math.addExact(Math.multiplyExact(instant.getEpochSecond(), 1000_000_000L), instant.getNano())
                ).toArray());
                PyArray_Descr descr = PyArray_DescrNewFromType(NPY_DATETIME);
                new PyArray_DatetimeDTypeMetaData(descr.c_metadata()).meta().base(NPY_FR_ns).num(1);
                return PyArray_NewFromDescr(arrayType, descr, 1, dims, null, data, NPY_ARRAY_CARRAY, null);
            } catch (ArithmeticException e) {
                throw new RuntimeException("Instant date range is outside of datetime64[ns] (1678-2262).", e);
            }
        } else if (value instanceof NpNdarrayByte) {
            builder.addType("NpNdarrayByte");
            NpNdarrayByte ndary = (NpNdarrayByte) value;
            SizeTPointer dims = new SizeTPointer(toLongArray(ndary.shape));
            SizeTPointer strides = new SizeTPointer(ndary.stridesInBytes());
            BytePointer data = new BytePointer(ndary.data);
            return PyArray_New(arrayType, ndary.ndim(), dims, NPY_BYTE, strides, data, 0, NPY_ARRAY_CARRAY, null);
        } else if (value instanceof NpNdarrayBoolean) {
            builder.addType("NpNdarrayBoolean");
            NpNdarrayBoolean ndary = (NpNdarrayBoolean) value;
            SizeTPointer dims = new SizeTPointer(toLongArray(ndary.shape));
            SizeTPointer strides = new SizeTPointer(ndary.stridesInBytes());
            BooleanPointer data = new BooleanPointer(ndary.data);
            return PyArray_New(arrayType, ndary.ndim(), dims, NPY_BOOL, strides, data, 0, NPY_ARRAY_CARRAY, null);
        } else if (value instanceof NpNdarrayChar) {
            builder.addType("NpNdarrayChar");
            NpNdarrayChar ndary = (NpNdarrayChar) value;
            SizeTPointer dims = new SizeTPointer(toLongArray(ndary.shape));
            SizeTPointer strides = new SizeTPointer(ndary.stridesInBytes());
            CharPointer data = new CharPointer(ndary.data);
            return PyArray_New(arrayType, ndary.ndim(), dims, NPY_USHORT, strides, data, 0, NPY_ARRAY_CARRAY, null);
        } else if (value instanceof NpNdarrayShort) {
            builder.addType("NpNdarrayShort");
            NpNdarrayShort ndary = (NpNdarrayShort) value;
            SizeTPointer dims = new SizeTPointer(toLongArray(ndary.shape));
            SizeTPointer strides = new SizeTPointer(ndary.stridesInBytes());
            ShortPointer data = new ShortPointer(ndary.data);
            return PyArray_New(arrayType, ndary.ndim(), dims, NPY_SHORT, strides, data, 0, NPY_ARRAY_CARRAY, null);
        } else if (value instanceof NpNdarrayInt) {
            builder.addType("NpNdarrayInt");
            NpNdarrayInt ndary = (NpNdarrayInt) value;
            SizeTPointer dims = new SizeTPointer(toLongArray(ndary.shape));
            SizeTPointer strides = new SizeTPointer(ndary.stridesInBytes());
            IntPointer data = new IntPointer(ndary.data);
            return PyArray_New(arrayType, ndary.ndim(), dims, NPY_INT, strides, data, 0, NPY_ARRAY_CARRAY, null);
        } else if (value instanceof NpNdarrayLong) {
            builder.addType("NpNdarrayLong");
            NpNdarrayLong ndary = (NpNdarrayLong) value;
            SizeTPointer dims = new SizeTPointer(toLongArray(ndary.shape));
            SizeTPointer strides = new SizeTPointer(ndary.stridesInBytes());
            LongPointer data = new LongPointer(ndary.data);
            return PyArray_New(arrayType, ndary.ndim(), dims, NPY_LONGLONG, strides, data, 0, NPY_ARRAY_CARRAY, null);
        } else if (value instanceof NpNdarrayFloat) {
            builder.addType("NpNdarrayFloat");
            NpNdarrayFloat ndary = (NpNdarrayFloat) value;
            SizeTPointer dims = new SizeTPointer(toLongArray(ndary.shape));
            SizeTPointer strides = new SizeTPointer(ndary.stridesInBytes());
            FloatPointer data = new FloatPointer(ndary.data);
            return PyArray_New(arrayType, ndary.ndim(), dims, NPY_FLOAT, strides, data, 0, NPY_ARRAY_CARRAY, null);
        } else if (value instanceof NpNdarrayDouble) {
            builder.addType("NpNdarrayDouble");
            NpNdarrayDouble ndary = (NpNdarrayDouble) value;
            SizeTPointer dims = new SizeTPointer(toLongArray(ndary.shape));
            SizeTPointer strides = new SizeTPointer(ndary.stridesInBytes());
            DoublePointer data = new DoublePointer(ndary.data);
            return PyArray_New(arrayType, ndary.ndim(), dims, NPY_DOUBLE, strides, data, 0, NPY_ARRAY_CARRAY, null);
        } else if (value instanceof NpNdarrayInstant) {
            builder.addType("NpNdarrayInstant");
            try {
                NpNdarrayInstant ndary = (NpNdarrayInstant) value;
                SizeTPointer dims = new SizeTPointer(toLongArray(ndary.shape));
                SizeTPointer strides = new SizeTPointer(ndary.stridesInBytes());
                LongPointer data = new LongPointer(Arrays.stream(ndary.data).mapToLong(instant ->
                        Math.addExact(Math.multiplyExact(instant.getEpochSecond(), 1000_000_000L), instant.getNano())
                ).toArray());
                PyArray_Descr descr = PyArray_DescrNewFromType(NPY_DATETIME);
                new PyArray_DatetimeDTypeMetaData(descr.c_metadata()).meta().base(NPY_FR_ns).num(1);
                return PyArray_NewFromDescr(arrayType, descr, ndary.ndim(), dims, strides, data, NPY_ARRAY_CARRAY, null);
            } catch (ArithmeticException e) {
                throw new RuntimeException("Instant date range is outside of datetime64[ns] (1678-2262).", e);
            }
        } else if (value instanceof Map) {
            builder.addType("Map");
            builder.tab++;

            @SuppressWarnings("unchecked")
            Map<Object, Object> map = (Map<Object, Object>) value;
            PyObject obj = PyDict_New();
            map.forEach((key, v) -> {
                builder.addType("Map.Entry");
                builder.tab++;
                PyDict_SetItem(obj, toPyObject(key, builder), toPyObject(v, builder));
                builder.tab--;
            });

            builder.tab--;
            return obj;
        } else if (value instanceof scala.collection.Map) {
            builder.addType("scala.collection.Map");
            builder.tab++;

            @SuppressWarnings("unchecked")
            scala.collection.Map<Object, Object> map = (scala.collection.Map<Object, Object>) value;
            PyObject obj = PyDict_New();
            map.foreachEntry((key, v) -> {
                builder.addType("Map.Entry");
                builder.tab++;
                PyDict_SetItem(obj, toPyObject(key, builder), toPyObject(v, builder));
                builder.tab--;
                return null;
            });

            builder.tab--;
            return obj;
        } else if (value instanceof Object[]) {
            builder.addType("Object[]");
            builder.tab++;

            Object[] ary = (Object[]) value;
            PyObject obj = PyList_New(ary.length);
            for (int i = 0; i < ary.length; i++) {
                PyList_SetItem(obj, i, toPyObject(ary[i], builder));
            }

            builder.tab--;
            return obj;
        } else if (value instanceof Iterable) {
            builder.addType("Iterable(" + value.getClass().getName() + ")");
            builder.tab++;

            @SuppressWarnings("unchecked")
            Iterable<Object> iter = (Iterable<Object>) value;
            PyObject obj = PyList_New(0);
            iter.forEach(v -> PyList_Append(obj, toPyObject(v, builder)));

            builder.tab--;
            return obj;
        } else if (value instanceof scala.Function0) {
            builder.addType("scala.Function0");
            @SuppressWarnings("unchecked")
            scala.Function0<Object> fn = (scala.Function0<Object>) value;
            return toPyCFunction(args -> fn.apply());
        } else if (value instanceof scala.Function1) {
            builder.addType("scala.Function1");
            @SuppressWarnings("unchecked")
            scala.Function1<Object, Object> fn = (scala.Function1<Object, Object>) value;
            return toPyCFunction(args -> fn.apply(args[0]));
        } else if (value instanceof scala.Function2) {
            builder.addType("scala.Function2");
            @SuppressWarnings("unchecked")
            scala.Function2<Object, Object, Object> fn = (scala.Function2<Object, Object, Object>) value;
            return toPyCFunction(args -> fn.apply(args[0], args[1]));
        } else if (value instanceof scala.Function3) {
            builder.addType("scala.Function3");
            @SuppressWarnings("unchecked")
            scala.Function3<Object, Object, Object, Object> fn = (scala.Function3<Object, Object, Object, Object>) value;
            return toPyCFunction(args -> fn.apply(args[0], args[1], args[2]));
        } else if (value instanceof scala.Function4) {
            builder.addType("scala.Function4");
            @SuppressWarnings("unchecked")
            scala.Function4<Object, Object, Object, Object, Object> fn = (scala.Function4<Object, Object, Object, Object, Object>) value;
            return toPyCFunction(args -> fn.apply(args[0], args[1], args[2], args[3]));
        } else if (value instanceof scala.Function5) {
            builder.addType("scala.Function5");
            @SuppressWarnings("unchecked")
            scala.Function5<Object, Object, Object, Object, Object, Object> fn = (scala.Function5<Object, Object, Object, Object, Object, Object>) value;
            return toPyCFunction(args -> fn.apply(args[0], args[1], args[2], args[3], args[4]));
        } else if (value instanceof scala.Function6) {
            builder.addType("scala.Function6");
            @SuppressWarnings("unchecked")
            scala.Function6<Object, Object, Object, Object, Object, Object, Object> fn = (scala.Function6<Object, Object, Object, Object, Object, Object, Object>) value;
            return toPyCFunction(args -> fn.apply(args[0], args[1], args[2], args[3], args[4], args[5]));
        } else if (value instanceof scala.Function7) {
            builder.addType("scala.Function7");
            @SuppressWarnings("unchecked")
            scala.Function7<Object, Object, Object, Object, Object, Object, Object, Object> fn = (scala.Function7<Object, Object, Object, Object, Object, Object, Object, Object>) value;
            return toPyCFunction(args -> fn.apply(args[0], args[1], args[2], args[3], args[4], args[5], args[6]));
        } else if (value instanceof scala.Function8) {
            builder.addType("scala.Function8");
            @SuppressWarnings("unchecked")
            scala.Function8<Object, Object, Object, Object, Object, Object, Object, Object, Object> fn = (scala.Function8<Object, Object, Object, Object, Object, Object, Object, Object, Object>) value;
            return toPyCFunction(args -> fn.apply(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7]));
        } else if (value instanceof scala.Function9) {
            builder.addType("scala.Function9");
            @SuppressWarnings("unchecked")
            scala.Function9<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object> fn = (scala.Function9<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object>) value;
            return toPyCFunction(args -> fn.apply(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8]));
        } else if (value instanceof scala.Function10) {
            builder.addType("scala.Function10");
            @SuppressWarnings("unchecked")
            scala.Function10<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object> fn = (scala.Function10<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object>) value;
            return toPyCFunction(args -> fn.apply(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9]));
        } else if (value instanceof scala.Function11) {
            builder.addType("scala.Function11");
            @SuppressWarnings("unchecked")
            scala.Function11<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object> fn = (scala.Function11<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object>) value;
            return toPyCFunction(args -> fn.apply(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10]));
        } else if (value instanceof scala.Function12) {
            builder.addType("scala.Function12");
            @SuppressWarnings("unchecked")
            scala.Function12<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object> fn = (scala.Function12<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object>) value;
            return toPyCFunction(args -> fn.apply(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11]));
        } else if (value instanceof scala.Function13) {
            builder.addType("scala.Function13");
            @SuppressWarnings("unchecked")
            scala.Function13<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object> fn = (scala.Function13<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object>) value;
            return toPyCFunction(args -> fn.apply(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12]));
        } else if (value instanceof scala.Function14) {
            builder.addType("scala.Function14");
            @SuppressWarnings("unchecked")
            scala.Function14<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object> fn = (scala.Function14<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object>) value;
            return toPyCFunction(args -> fn.apply(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13]));
        } else if (value instanceof scala.Function15) {
            builder.addType("scala.Function15");
            @SuppressWarnings("unchecked")
            scala.Function15<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object> fn = (scala.Function15<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object>) value;
            return toPyCFunction(args -> fn.apply(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13], args[14]));
        } else if (value instanceof scala.Function16) {
            builder.addType("scala.Function16");
            @SuppressWarnings("unchecked")
            scala.Function16<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object> fn = (scala.Function16<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object>) value;
            return toPyCFunction(args -> fn.apply(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13], args[14], args[15]));
        } else if (value instanceof scala.Function17) {
            builder.addType("scala.Function17");
            @SuppressWarnings("unchecked")
            scala.Function17<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object> fn = (scala.Function17<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object>) value;
            return toPyCFunction(args -> fn.apply(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13], args[14], args[15], args[16]));
        } else if (value instanceof scala.Function18) {
            builder.addType("scala.Function18");
            @SuppressWarnings("unchecked")
            scala.Function18<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object> fn = (scala.Function18<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object>) value;
            return toPyCFunction(args -> fn.apply(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13], args[14], args[15], args[16], args[17]));
        } else if (value instanceof scala.Function19) {
            builder.addType("scala.Function19");
            @SuppressWarnings("unchecked")
            scala.Function19<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object> fn = (scala.Function19<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object>) value;
            return toPyCFunction(args -> fn.apply(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13], args[14], args[15], args[16], args[17], args[18]));
        } else if (value instanceof scala.Function20) {
            builder.addType("scala.Function20");
            @SuppressWarnings("unchecked")
            scala.Function20<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object> fn = (scala.Function20<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object>) value;
            return toPyCFunction(args -> fn.apply(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13], args[14], args[15], args[16], args[17], args[18], args[19]));
        } else if (value instanceof scala.Function21) {
            builder.addType("scala.Function21");
            @SuppressWarnings("unchecked")
            scala.Function21<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object> fn = (scala.Function21<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object>) value;
            return toPyCFunction(args -> fn.apply(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13], args[14], args[15], args[16], args[17], args[18], args[19], args[20]));
        } else if (value instanceof scala.Function22) {
            builder.addType("scala.Function22");
            @SuppressWarnings("unchecked")
            scala.Function22<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object> fn = (scala.Function22<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object>) value;
            return toPyCFunction(args -> fn.apply(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13], args[14], args[15], args[16], args[17], args[18], args[19], args[20], args[21]));
        }
        builder.addType(value.getClass().getName() + "  <- Unsupported");
        throw new PythonException("Cannot convert the Java object to a Python object.\n" +
                "\nValue type tree\n" + builder.toString() +
                "\nvalue = " + value);
    }

    private static PyObject toPyCFunction(Function<Object[], Object> fn) {
        PyCFunction pyFunc = new PyCFunction() {
            @Override
            public PyObject call(PyObject self, PyObject args) {
                try {
                    TypeTreeBuilder builderToJava = new TypeTreeBuilder(1);
                    builderToJava.addType("(arguments)");
                    builderToJava.tab++;
                    Object[] objs = new Object[(int) PyTuple_Size(args)];
                    for (int i = 0; i < objs.length; i++) {
                        objs[i] = toJava(PyTuple_GetItem(args, i), builderToJava);
                    }
                    builderToJava.tab--;

                    TypeTreeBuilder builderToPython = new TypeTreeBuilder(1);
                    builderToPython.addType("(return value)");
                    builderToPython.tab++;
                    PyObject pyObject = toPyObject(fn.apply(objs), builderToPython);
                    builderToPython.tab--;
                    return pyObject;
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
        PyMethodDef methodDef = new PyMethodDef().
                ml_name(new BytePointer("org.bytedeco.embeddedpython")).
                ml_meth(pyFunc).
                ml_flags(METH_VARARGS);
        return PyCFunction_NewEx(methodDef, null, MAIN_MODULE);
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

    /**
     * Don't forget to call Py_DecRef().
     */
    private static PyObject getIter(PyObject obj) {
        PyObject iterator = PyObject_GetIter(obj);
        if (iterator == null) {
            PyErr_Clear();
        }
        return iterator;
    }
}
