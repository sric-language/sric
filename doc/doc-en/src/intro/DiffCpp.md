## From C++ to Sric
### Types

| C++  | Sric  |
| ----- | ---- |
| int | Int |
| short | Int16 |
| int32_t | Int32 |
| unsigned int | UInt32 |
| int64_t | Int64 |
| uint64_t | UInt64 |
| float | Float/Float32 |
| double | Float64 |
| void | Void |
| char | Int8 |

### Defines
| C++  | Sric  |
| ----- | ---- |
| const char* str | var str: raw* Int8 |
| void foo(int i) {} | fun foo(i: Int) {} |
| char a[4] | var a: [4]Int8 |
| const int& a | var a: & const Int |

### Class

C++
```
#include <math.h>

class Point {
public:
    int x
    int y
    double dis(const Point &t) const {
        int dx = t.x - x;
        int dy = t.y - y;
        return sqrt(dx*dx + dy*dy);
    }
};
```
Sric:
```
import sric::*

struct Point {
    var x: Int
    var y: Int
    fun dis(t: & const Point) const: Float {
        var dx = t.x - x
        var dy = t.y - y
        return sqrt(dx*dx + dy*dy)
    }
};
```


## Features Compare

### Removed features from C++

- No function overload by params
- No header file
- No implicit copying of large objects
- No define multi var per statement
- No nested class, nested function
- No class, only struct
- No namespace
- No macro
- No forward declarations
- No three static
- No friend class
- No multiple inheritance
- No virtual,private inheritance
- No i++ only ++i
- No switch auto fallthrough
- No template specialization
- No various constructors

### More than C++

- Simple and easy
- Memory safe
- Modularization
- With block
- Non-nullable pointer
- Dynamic reflection
- Named args
