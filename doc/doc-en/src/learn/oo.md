## Struct

Classes don't have parameterized constructors - callers must initialize them manually:
```
struct Point {
    var x: Int = 0;
    var y: Int = uninit;
    var z: Int;
}
var p = Point { .y = 1; };
```
Initialization uses with blocks (different from C's named initialization). These blocks can contain any statements and work in non-initialization contexts:

```
var point = Point { .y = 1; };
point {
    .x = 2; if (a) { .y = 3; }
}
```
Alternatively, provide an initialization method (conventionally named init):
```
struct Point {
    var x: Int;
    var y: Int;

    fun init(a: Int) {
        this { .y = a; }
    }
}
var p = Point { .init(1); };
```
## Methods
- Types can have methods (non-static member functions with implicit this pointer)
- Methods must appear after data members
- Mutability modifier goes after function name:
```
struct Point {
    fun length() const : Float {
        ...
    }
}
```
Structs can contain static functions/fields (no implicit this):

```
struct Point {
    static fun foo() {
    }
}
Point::foo();
```
## Inheritance
- No multiple inheritance (Java-like)
- Base classes must be marked virtual or abstract
- Traits (like Java interfaces) can't have data members or method implementations
- Inheritance list: class first (if any), then traits
- Overriding requires override marker
- Use super to call parent methods
```
virtual struct B {
    virtual fun foo() {}
}
trait I {
    abstract fun foo2();
}
struct A : B , I {
    override fun foo2() {}
}
```
## Constructors/Destructors
Sric has no C++-style constructors - only parameterless default initialization:
```
struct A {
    fun new() {
    }
    fun delete() {
    }
}
```
Destructors are rarely needed due to automatic memory management via ownership. Constructors exist mainly to handle complex initialization logic:
```
struct A {
    var p : own* Int = ...;
    fun new() {
        p = new Int;
    }
}
```
