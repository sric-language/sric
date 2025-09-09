
## 闭包/Lambda
匿名函数使用fun关键字定义
```
fun foo(f: fun(a:Int):Int) {
    f(1)
}

foo(fun(a:Int):Int{
    printf("%d\n", a)
    return a + 1
});
```
暂不支持闭包的返回类型推断，需要显式指定返回类型。

## 捕获变量
默认通过值来捕获外部变量。
```
var i = 0
var f = fun(a:Int):Int{
    return a + i
};
```

## 静态闭包
静态闭包指无状态的闭包，用static修饰。不可捕获变量。
```
var f : fun(a:Int) static : Int
```

## 引用捕获
不支持C++的引用捕获。如果想引用捕获，可以自己取地址。
```
var i = 0
var ri = &i
var f = fun(a:Int):Int{
    return a + *ri
};
```

## 移动捕获

不支持C++的移动捕获。AutoMove用来包装对象，避免显式使用move指令：

```
var arr: DArray;
var autoMove = AutoMove { .set(arr) }

var f = fun() {
    var s = autoMove.get().size()
}

```

