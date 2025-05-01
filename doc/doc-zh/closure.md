
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