## Statements
- All statements end with semicolons
- No `do while` or `goto` (otherwise same as C++)
- `switch` doesn't fall through by default (use `fallthrough` explicitly)
```sric
switch (i) {
    case 1:
        fallthrough;
    case 2:
        printf("%d\n", i);
}
```
### Unsafe
Dereference raw pointers in unsafe blocks:

```sric
var p: *Int;
...
unsafe {
    var i = *p;
}
```
Unsafe functions require unsafe blocks:

```sric
unsafe fun foo() { ... }

fun main() {
    unsafe {
        foo();
    }
}
```
### Expressions
Operator precedence matches C/C++ except bitwise operators have higher precedence than comparisons

```sric
if (i & Mask != 0) {}
// Equivalent to:
if ((i & Mask) != 0) {}
```
Only prefix ++i is supported (no postfix i++)

### With Blocks
With blocks (unlike C++ designated initializers) can contain any statements:

```sric
struct A {
    var i: Int;
    fun init() { ... }
}

var a = A { .init(); .i = 0; };
var a: own* A = new A { .i = 0; };
```
## Pointer Access
Use . for both direct and pointer access (no ->):

```sric
var a: A;
var b: own* A;
a.foo();
b.foo();
```
## Type Conversion/Checking
as for dynamic/numeric conversion, is for type checking:

```sric
var a = p as own* A;
var b = p is own* A;
```
Other conversions use unsafeCast.

### Error Handling
Sric has no exception handling (won't catch C++ exceptions either).
