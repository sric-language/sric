## Generics

### Generic Definition
Unlike C++, generics use `$<` prefix to disambiguate between type parameters and the less-than operator.
```sric
struct Tree$<T> {
}
```
Type parameters can have example types for compile-time type checking:

```
abstract struct Linkable$<T> {
    var next: own*? T;
    var previous: *? T;
}

struct LinkedList$<T: Linkable$<T>> {
}
```
### Template Instantiation
When instantiating generic templates, any type satisfying the example type constraints can be used:

```
var tree = Tree$<Int> {};
```
