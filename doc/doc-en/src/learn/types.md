## Value Types

By default, variables in Sric are value types that get automatically copied during assignment/passing. Pointer types only copy the pointer itself, not the pointed-to object.

## Pointers
- Three pointer types: owning, non-owning, and raw
- Asterisk placement differs from C/C++ (before type)

```sric
var p: Int            // Value type
var p: own* Int       // Owning pointer
var p: * Int          // Non-owning pointer
var p: & Int          // Reference
var p: raw* Int       // Raw pointer
var p: uniq* Int      // Unique pointer
```

Sric also provides C++-style smart pointers (SharedPtr, WeakPtr) via standard library.

### Memory Allocation
Get pointers via address-of operator or `new`:

```sric
var i: own* Int = new Int  // Parentheses omitted (no constructors)
```

#### Owning Pointers
- Own their objects (auto-released at scope exit)
- Require explicit move/copy during transfer:

```sric
var p1: own* Int = ...
var p2 = move p1          // Transfer ownership
var p3 = share(p1)        // Shared ownership
```

#### Unique Pointers
`uniq*` is zero-overhead. Similar to `own*`, but without a share() method.

```sric
var p1: uniq* Int = makeUniq$<T>()
var p2 = move p1
```

#### Non-owning Pointers
- No borrowing restrictions like Rust
- Runtime validity checks
- Implicit conversion from `own*`/`uniq*`:

```sric
var p1: own* Int = ...
var p4: * Int = p1       // Non-owning
var p5: raw* Int = p1    // Raw
```

#### Raw Pointers
- C/C++-style pointers (require `unsafe`):
```sric
var p: raw* Int = ...
unsafe {
    var i = *p
}
```

## Address-of Operator
Get pointers from values (non-owning by default):

```sric
var i: Int = 0
var p: *Int = &i
```

## Pointer Arithmetic
Only allowed for raw pointers in `unsafe` contexts:

```sric
var p : raw* Int = ...
unsafe {
    ++p
    p = p + 2
}
```

## References
Similar to C++ but restricted to function parameters/returns:

```sric
fun foo(a: & const Int) {}  // Auto-dereferencing
```

## Arrays
Fixed-size arrays (for dynamic arrays see `DArray`):

```sric
var a: [5]Int                // Explicit size
var a = []Int { 1,3,4 }      // Size inference
constexpr var size = 15
var a: [size]Int             // Constexpr size
```

## Null Safety
Pointers are non-nullable by default (use `?` for nullable):

```sric
var a: own*? B = null     // Valid
var a: own* B = null      // Compile error
```

## Immutability
`const` can modify either pointer or pointee:

```sric
var p : raw* const Int     // Immutable value
var p : const raw* Int     // Immutable pointer
var p : const raw* const Int  // Both
```
