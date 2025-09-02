## Standard Library

### Overview
- `sric`: Built-in standard library  
- `jsonc`: JSON parsing/compression  
- `serial`: Serialization using dynamic reflection  

[API Documentation](/apidoc.html)  

### Using C Libraries  
The `sric` module only exports common C functions.  

To use unexported C functions:  
```sric
externc fun printf(format: raw* const Int8, args: ...);

fun main() {
    printf("Hello World\n");
}
```
Declare macros as const variables. See C++ Interop for details.

### String
Strings are raw* const Int8 but auto-convert to String:

```sric
var str: String = "abc";
```
Explicit conversion when needed:

```sric
var str = asStr("abc");
```
### DArray
Dynamic array (like C++ std::vector):

```sric
var a : DArray$<Int>;
a.add(1);
a.add(2);
verify(a[0] == 1);
```
## HashMap
Key-value storage:

```sric
var map = HashMap$<Int, String>{};
map.set(1, "1");
map.set(2, "2");
verify(map[2] == "2");
```
## File I/O
Using FileStream:

Write:
```sric
var stream = FileStream::open("tmp.txt", "wb");
stream.writeStr("Hello\nWorld");
```
Read:

```sric
var stream = FileStream::open("tmp.txt", "rb");
var line = stream.readAllStr();
```
Mode strings match C's fopen().
