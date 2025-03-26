
### Data Type

```
var p: Int             //value type
var p: own* Int;       //ownership pointer
var p: ref* Int;       //non-owning pointer
var p: & Int;          //reference
var p: raw* Int;       //unsafe raw pointer
```


### Explicit Copy or Move

Move or share ownership pointer
```
var p: own* Int = ...;
var p1 = p; //compiler error;
var p2 = move p;
var p3 = share(p2);
```

Move or copy if the struct has ownership pointer:
```
struct A {
    var i: own* Int;
    fun copy(): A { ... }
}
var a: A;
var x = a; //compile error
var b = move a;
var c = a.copy();
```

### Unsafe
Dereference raw pointer in unsafe block

```
var p: raw* Int;
...
unsafe {
    var i = *p;
}
```

Unsafe function must call in unsafe block
```
unsafe fun foo() { ... }

fun main() {
    unsafe {
        foo();
    }
}
```

### Inheritance

Single inheritance
```
trait I {
    virtual fun foo();
}

virtual struct B {
    var a: Int;
    fun bar() { ... }
}

struct A : B, I {
    override fun foo(B* b) {
        ...
    }
}

```

### With Block

The with Block is not C++ designated initialization. It can contain any statement.
```
struct A {
    var i: Int;
    fun init() { ... }
}

var a  = A { .init(); .i = 0; };
var a: own* A = new A { .i = 0; };
```


### Pointer Usage

Always access by '.'
```
var a: A;
var b: own* A;
a.foo();
b.foo();
```

### Type Cast and Check
```
var a = p as own* A;
var b = p is own* A;
```

### Array

static size Array
```
var a  = []Int { 1,2,3 };
var a: [15]Int;
```


### Generic Type
Generic params start with '$<'
```
struct Bar$<T> {
    fun foo() {
        ...
    }
}

T fun foo$<T>(a: T) {
    return a;
}

var b: Bar$<Int>;
```

### Null safe

Pointer is non-nullable by default.
```
var a: own*? B;
var b: own* B = a;
```


### Immutable

Just like C++
```
var p : raw* const Int;
var p : const raw* Int;
var p : const raw* const Int;
```


### Protection
```
public
private
protected
readonly
```
'readonly' means public read access, private write access.

### Operator Overloading

```
struct A {
    operator fun mult(a: A): A { ... }
}

var c = a * b;
```

operator methods:
```
methods    symbol
------     ------
plus       a + b 
minus      a - b 
mult       a * b 
div        a / b 
get        a[b] 
set        a[b] = c
compare    == != < > <= >=
```

### Module

Module is namespace as well as the unit of compilation and deployment.

A module contains several source files and folders.

The module is defined in build scripts:
```
name = std
summary = standard library
outType = lib
version = 1.0
depends = sys 1.0
srcDirs = src/
```

import external module in code:
```
import std::*;
import std::Vec;
```

### Closure

```
fun foo(f: fun(a:Int) ) {
    f(1);
}

foo(fun(a:Int){ ... });
```

### Typealias

typealias:
```
typealias VecInt = std::Vec$<Int>;
```

### Enum

```
enum Color {
    red = 1, green, blue
}

var c = Color::red;
```

### Default Param and Named Arg

```
fun foo(a: Int, b: Int = 0) {
}

fun main() {
    foo(a : 10);
}
```
