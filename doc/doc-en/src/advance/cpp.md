## Interoperability with C++

Sric can easily interact with C++. It compiles to human-readable C++ code that can be directly called like regular C++ code.

To call C++ code, simply declare the function prototypes. Use:
- `externc` for C-style/no-namespace code
- `extern` for matching namespaces
- Symbol mapping for other cases

### C-style/No Namespace
```sric
externc fun printf(format: raw* const Int8, args: ...);

fun main() {
    printf("Hello World\n");
}
```
### Matching Namespaces
When C++ namespace matches Sric module name:
C++:
```cpp
namespace xx {
    class P {
        void foo();
    };
}
```
Sric:

```sric
//xx module
extern struct P {
    fun foo();
}
```
Module name must match C++ namespace.

### Symbol Mapping
Use symbol annotation to map symbols:
C++:

```cpp
namespace test {
    void hi() {
    }
}
```
Sric:

```sric
//@extern symbol: test::hi
extern fun hello();
```
Calling hello() in Sric invokes C++'s hi().

### Header Inclusion
Use @#include annotation to include C++ headers:

```sric
//@#include "test.h"
```
### Parameterized Constructors
Since Sric doesn't support parameterized constructors, use makePtr/makeValue instead.

### Complete Example
```sric
import sric::*;

//@#include <vector>
//@extern symbol: std::vector
extern struct vector$<T> {
    fun size(): Int;
}

fun testExtern() {
    var v = makePtr$<vector$<Int>>(3);
    verify(v.size() == 3);
}

fun testExtern2() {
    var v2 = makeValue$<vector$<Int>>(3);
    verify(v2.size() == 3);
}
```
### Generating Sric Interfaces from C++ Headers
Use Python scripts in the tool directory to generate Sric prototypes from C++ headers.

### Compiling Without fmake
You can manually compile generated C++ code in sric/output directory.

Define these macros:
- SC_CHECK: Enable safety checks
- SC_NO_CHECK: Disable safety checks

If neither is defined, they're automatically set based on _DEBUG/NDEBUG.

### Mixed Sric/C++ Compilation
Add fmake configurations in module.scm (prefix with fmake.):

```sric
fmake.srcDirs = ./
fmake.incDirs = ./
```