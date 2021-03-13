# JavaCPP Embedded Python

With this library, you can embed Python to your Java or Scala project.
This library is a wrapper of javacpp-presets/cpython.
The main purpose of this library is to use Python libraries from Java or Scala.

## Javadoc

http://bytedeco.org/javacpp-embedded-python/apidocs/

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
| bool<br>scalar np.bool8 | boolean |
| scalar np.int8 | byte |
| scalar np.int16 | short |
| scalar np.uint16 | char |
| scalar np.int32 | int |
| int<br>scalar np.int64 | long |
| scalar np.float32 | float |
| float<br>scalar np.float64 | double |
| scalar np.datetime64[W, D, h, m, s, ms, us, or ns] | Instant |
| str | String |
| bytes<br>bytearray | byte[] |
| Tuple<br>List | Object[] |
| Dict | Map |
| ndarray np.int8 | NpNdarrayByte |
| ndarray np.bool8 | NpNdarrayBoolean |
| ndarray np.int16 | NpNdarrayShort |
| ndarray np.uint16 | NpNdarrayChar |
| ndarray np.int32 | NpNdarrayInt |
| ndarray np.int64 | NpNdarrayLong |
| ndarray np.float32 | NpNdarrayFloat |
| ndarray np.float64 | NpNdarrayDouble |
| ndarray np.datetime64[W, D, h, m, s, ms, us, or ns] | NpNdarrayInstant |

If you want to use a Pandas DataFrame, please use ```DataFrame.to_dict('list')```.

### Java to Python

| Java | Python |
|--------|------|
| null | None |
| boolean | bool |
| byte<br>short<br>char<br>int<br>long | int |
| float<br>double | float |
| Instant | np.datetime64[ns] |
| String | str |
| Object[]<br>Iterable | List |
| Map | Dict |
| byte[] | bytes |
| boolean[]<br>NpNdarrayBoolean | np.ndarray, dtype=np.bool8 |
| NpNdarrayByte | np.ndarray, dtype=np.int8 |
| short[]<br>NpNdarrayShort | np.ndarray, dtype=np.int16 |
| char[]<br>NpNdarrayChar | np.ndarray, dtype=np.uint16 |
| int[]<br>NpNdarrayInt | np.ndarray, dtype=np.int32 |
| long[]<br>NpNdarrayLong | np.ndarray, dtype=np.int64 |
| float[]<br>NpNdarrayFloat | np.ndarray, dtype=np.float32 |
| double[]<br>NpNdarrayDouble | np.ndarray, dtype=np.float64 |
| Instant[]<br>NpNdarrayInstant | np.ndarray, dtype=np.datetime64[ns] |
| scala.Function0 - Function22 | global Python function |

## Exceptions

### Python to Java
When Python code throws exceptions in ```Python.eval()``` or ```Python.exec()```,
tracebacks are printed to stderr,
and ```PythonException``` is thrown in Java.

### Java to Python
When Java code throws exceptions in the Java lambda,
```Throwable.printStackTrace()``` is called,
and ```RuntimeError``` is thrown in Python.
