### Operator Overloading

Use the `operator` keyword to overload operators:

```sric
struct A {
    operator fun mult(a: A): A { ... }
}

var c = a * b;
```
Overloadable operators:
```
Methods    Symbols
------     ------
plus       a + b
minus      a - b
mult       a * b
div        a / b
get        a[b]
set        a[b] = c
compare    == != < > <= >=
add        a,b,c;
```
### Comma Operator
The comma operator only works within with blocks:

```sric
x { a, b, c }
```
This is equivalent to:
```sric
x { .add(a).add(b).add(c); }
```
