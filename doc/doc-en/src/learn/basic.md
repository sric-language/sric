### Built-in Types
- Int, Int8, Int16, Int32, Int64, UInt8, UInt16, UInt32, UInt64
- Float, Float32, Float64
- Bool
- Void

Int is 32-bit by default, Float is 32-bit by default.

### Strings
Strings can span multiple lines:
```
var s = "ab
            cd"
```
Triple-quoted strings don't require escaping double quotes:
```
var s = """ab"
            cd"""
```
String literals are of type raw*const Int8 and can be automatically converted to sric::String.

### Characters
A character represents a single letter and is of type Int8:
```
var c : Int8 = 'A'
```

### Comments
Single-line comment:
```
//comment
```

Multi-line comment:
```
/*
comment
*/
```

Documentation comment:
```
/**
introduction
*/
```

### Annotations
```
//@method: GET
```
Annotations can be dynamically accessed through reflection interfaces.

### Variable Declaration
- Use `var` to declare variables, regardless of mutability.
- Type annotations come after the variable name.
- Only one variable can be declared per statement.

```
var i: Int = 0
```

Type inference is only supported for local variables within functions:
```
var i = 0
```

Variables are automatically initialized to default values. Use `uninit` keyword to keep random values:
```
var i = uninit
```

Global variables must be immutable unless marked with `unsafe`:
```
var i: const Int = 0
```

### Function Definition
- Functions start with `fun`
- Return type can be omitted when it's Void
- Function names must be unique (no parameter-based overloading)

```
fun foo(a: Int): Int { return 0 }
fun foo2() {}
```

Default parameters and named parameters:
```
fun foo(a: Int, b: Int = 0) {}
foo(a: 1)
```
Named parameters improve readability by explicitly showing parameter names.

### Forward Declaration
There's no forward declaration like in C/C++. Functions can call others defined later in the code because Sric uses a multi-pass compiler architecture.

### Visibility
Variables and functions support visibility markers:
```
public
private
protected
readonly
```
Examples:
```
private fun foo() {}
readonly var i: Int = 0
```
- Visibility scope for global variables and functions is the current file. `private` makes them invisible to other files.
- Default visibility is `public` (no need to explicitly specify).
- `protected` means visible within the current module or to derived classes.
- `readonly` can only modify variables (not functions), meaning public read but private write access.
- 