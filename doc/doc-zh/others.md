

## 泛型
泛型和C++不同的是使用'$<'开头，这是为了消除泛型参数和小于运算符的歧义。
```
struct Tree$<T> {
}
```
泛型参数可以有示例类型，编译时以示例类型来做类型检查。
```
struct Compareable$<T> {
    operator fun compare(t: *T): Int;
}
strcut Tree$<T : Compareable> {
}
```
泛型模板实例化时，可以传入任意满足示例类型的类型。不需要继承示例类型。
```
var tree = Tree$<Int> {};
```
暂不支持泛型函数的泛型参数类型推断，需要显式写出泛型参数。
```
fun max$<T: Int>(a: T, b: T) {
    return a > b ? a : b;
}
var m = max$<Int>(0, 1);
```

## 闭包
匿名函数使用fun关键字定义
```
fun foo(f: fun(a:Int):Int) {
    f(1);
}

foo(fun(a:Int):Int{
    printf("%d\n", a);
    return a + 1;
});
```
暂不支持闭包的返回类型推断，需要显式制定返回类型。

默认通过值来捕获外部变量，不支持C++类似的引用捕获。
```
var i = 0;
var f = fun(a:Int):Int{
    return a + i;
};
```
如果想引用捕获，可以自己取地址。
```
var i = 0;
var ri = &i;
var f = fun(a:Int):Int{
    return a + *ri;
};
```

### 操作符重载

```
struct A {
    operator fun mult(a: A): A { ... }
}

var c = a * b;
```

可重载的操作符:
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


## 反射
默认启用反射，需要手动添加reflect标记。
```
reflect struct Point {
    var x: Int;
    var y: Int;
}
```
