# JavaCPP Embedded Python

With this library, you can embed Python to your Java or Scala project.
This library is a wrapper of javacpp-presets/cpython.
The main purpose of this library is to use Python libraries from Java or Scala.

## Usage

You can put the global variable ```a```.

```Java
Python.put("a", 1);
```

Execute the Python code.

```Java
Python.exec("b = a + 2");
```

Get the global variable ```b```.

```Java
long b = Python.get("b");
```

You can add a global function to Python.
The ```scala.Function2``` interface comes from scala-java8-compat library.
If you don't need the return value, please return ```null```.
This will return ```None``` in Python.

```Java
import scala.Function2;

Python.put("f", (Function2<Long, Long, Long>) (a, b) -> a + b);
long v = Python.eval("f(1, 2)");
```

You can also use a Numpy np.ndarray and convert it to a Java array.

```Java
NpNdarrayFloat ndary = Python.eval("np.arange(6, dtype=np.float32).reshape([2, 3])");
float[][] ary = ndary.toArray2d();
```

If you need a Python library, please use the Pip class.

```Java
Pip.install("pandas");
```

## Type mappings

### Python to Java

| Python | Java |
|--------|------|
| None | null |
| bool<br>np.bool8 scalar | boolean |
| np.int8 scalar | byte |
| np.int16 scalar | short |
| np.uint16 scalar | char |
| np.int32 scalar | int |
| int<br>np.int64 scalar | long |
| np.float32 scalar | float |
| float<br>np.float64 scalar | double |
| str | String |
| bytes<br>bytearray | byte[] |
| Tuple<br>List | Object[] |
| Dict | Map |
| np.int8 ndarray | NpNdarrayByte |
| np.bool8 ndarray | NpNdarrayBoolean |
| np.int16 ndarray | NpNdarrayShort |
| np.uint16 ndarray | NpNdarrayChar |
| np.int32 ndarray | NpNdarrayInt |
| np.int64 ndarray | NpNdarrayLong |
| np.float32 ndarray | NpNdarrayFloat |
| np.float64 ndarray | NpNdarrayDouble |

If you want to use a Pandas DataFrame, please use ```DataFrame.to_dict('list')```.

### Java to Python

| Java | Python |
|--------|------|
| null | None |
| boolean | bool |
| byte<br>short<br>char<br>int<br>long | int |
| float<br>double | float |
| String | str |
| Iterable | List |
| Map | Dict |
| byte[] | bytes |
| boolean[]<br>NpNdarrayBoolean | np.ndarray, dtype=np.bool8 |
| short[]<br>NpNdarrayShort | np.ndarray, dtype=np.int16 |
| char[]<br>NpNdarrayChar | np.ndarray, dtype=np.uint16 |
| int[]<br>NpNdarrayInt | np.ndarray, dtype=np.int32 |
| long[]<br>NpNdarrayLong | np.ndarray, dtype=np.int64 |
| float[]<br>NpNdarrayFloat | np.ndarray, dtype=np.float32 |
| double[]<br>NpNdarrayDouble | np.ndarray, dtype=np.float64 |
| scala.Function0 - Function6 | global Python function |
