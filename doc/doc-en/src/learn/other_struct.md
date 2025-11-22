## Type Aliases
Type aliases are equivalent to C's typedef:
```sric
typealias size_t = Int32
```
## Enums
Enums are similar to C++ but always scoped:

```sric
enum Color {
    Red, Green = 2, Blue
}

fun foo(c: Color) {}

foo(Color::Red)
```
Explicit size specification:
```sric
enum Color : UInt8 {
    Red, Green = 2, Blue
}
```

## Unsafe Structures
Unsafe structs match their C++ counterparts exactly, without safety check markers. Extern structs are unsafe by default.

Within unsafe structs, this is a raw pointer (not safe pointer). Objects allocated with new can be converted to safe pointers using rawToRef:

```sric
unsafe struct A {
    fun foo() {
        var self = rawToRef(this)
    }
}
```


## dconst Method

To reduce code duplication and complex features like function overloading, Sric provides the dconst method.

```
struct A {
    var i: String
    fun foo() dconst : * String {
        ...
        return &i
    }
}
```

The dconst method automatically generates both const and non-const versions internally in the compiler. This is equivalent to the following C++ code:

```
class A {
    string i;
public:
    string* foo() {
        ...
        return &i;
    }

    const string* foo() const {
        ...
        return &i;
    }
};
```
