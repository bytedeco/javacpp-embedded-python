package org.bytedeco.embeddedpython;

import org.bytedeco.cpython.PyObject;
import org.bytedeco.cpython.PyTypeObject;

import static org.bytedeco.cpython.global.python.*;
import static org.bytedeco.numpy.global.numpy.*;

class PyTypes {
    static final PyTypeObject noneType = _PyNone_Type();
    static final PyTypeObject boolType = PyBool_Type();
    static final PyTypeObject longType = PyLong_Type();
    static final PyTypeObject floatType = PyFloat_Type();
    static final PyTypeObject unicodeType = PyUnicode_Type();
    static final PyTypeObject bytesType = PyBytes_Type();
    static final PyTypeObject byteArrayType = PyByteArray_Type();
    static final PyTypeObject dictType = PyDict_Type();
    static final PyTypeObject boolArrType = PyBoolArrType_Type();
    static final PyTypeObject byteArrType = PyByteArrType_Type();
    static final PyTypeObject ushortArrType = PyUShortArrType_Type();
    static final PyTypeObject shortArrType = PyShortArrType_Type();
    static final PyTypeObject intArrType = PyIntArrType_Type();
    static final PyTypeObject longArrType = PyLongArrType_Type();
    static final PyTypeObject floatArrType = PyFloatArrType_Type();
    static final PyTypeObject doubleArrType = PyDoubleArrType_Type();
    static final PyTypeObject datetimeArrType = PyDatetimeArrType_Type();
    static final PyTypeObject arrayType = PyArray_Type();

    private PyTypes() {
    }

    static PyTypeObject Py_TYPE(PyObject ob) {
        return ob.ob_type();
    }

    static boolean Py_IS_TYPE(PyObject ob, PyTypeObject type) {
        return Py_TYPE(ob).equals(type);
    }

    static boolean PyType_HasFeature(PyTypeObject type, long feature) {
        return ((type.tp_flags() & feature) != 0);
    }

    static boolean PyType_FastSubclass(PyTypeObject type, long flag) {
        return PyType_HasFeature(type, flag);
    }

    static boolean PyObject_TypeCheck(PyObject ob, PyTypeObject type) {
        return Py_IS_TYPE(ob, type) || (PyType_IsSubtype(Py_TYPE(ob), type) != 0);
    }

    static boolean PyNone_Check(PyObject x) {
        return Py_IS_TYPE(x, noneType);
    }

    static boolean PyBool_Check(PyObject x) {
        return Py_IS_TYPE(x, boolType);
    }

    static boolean PyLong_Check(PyObject op) {
        return PyType_FastSubclass(Py_TYPE(op), Py_TPFLAGS_LONG_SUBCLASS);
    }

    static boolean PyFloat_Check(PyObject op) {
        return PyObject_TypeCheck(op, floatType);
    }

    static boolean PyUnicode_Check(PyObject op) {
        return PyType_FastSubclass(Py_TYPE(op), Py_TPFLAGS_UNICODE_SUBCLASS);
    }

    static boolean PyBytes_Check(PyObject op) {
        return PyType_FastSubclass(Py_TYPE(op), Py_TPFLAGS_BYTES_SUBCLASS);
    }

    static boolean PyByteArray_Check(PyObject op) {
        return PyObject_TypeCheck(op, byteArrayType);
    }

    static boolean PyDict_Check(PyObject op) {
        return PyType_FastSubclass(Py_TYPE(op), Py_TPFLAGS_DICT_SUBCLASS);
    }
}
