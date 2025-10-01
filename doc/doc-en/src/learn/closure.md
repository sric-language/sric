## Closures/Lambdas
Anonymous functions are defined using the `fun` keyword:
```
fun foo(f: fun(a:Int):Int) {
    f(1)
}

foo(fun(a:Int):Int{
    printf("%d\n", a)
    return a + 1
})
```
Return type inference is not yet supported for closures - the return type must be explicitly specified.

## Variable Capture
By default, external variables are captured by value:
```
var i = 0
var f = fun(a:Int):Int{
    return a + i
}
```

## Mutable
Variables captured by default are immutable.
```
var i = 0
var f = fun(a:Int) mut {
    i = 1
}
```

## Static Closures
Static closures are state-less closures marked with `static`. They cannot capture variables:
```
var f : fun(a:Int) static : Int
```

## Reference Capture
C++-style reference capture is not supported. For reference capture, you need to explicitly take the address:
```
var i = 0
var ri = &i
var f = fun(a:Int):Int{
    return a + *ri
};
```

## Move Capture
C++-style move capture is not supported. Use `AutoMove` to wrap objects and avoid explicit move instructions:
```
var arr: DArray
var autoMove = AutoMove { .set(arr) }

var f = fun() {
    var s = autoMove.get().size()
}
```
