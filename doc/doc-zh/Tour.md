
### 数据类型

```
var p: Int             //值类型
var p: refable Int;    //可引用的值类型
var p: own* Int;       //所有权指针
var p: ref* Int;       //非所有权指针
var p: & Int;          //引用
var p: raw* Int;       //裸指针
```


### 明确的拷贝或者移动

移动或者共享所有权指针
```
var p: own* Int = ...;
var p1 = p; //compiler error;
var p2 = move p;
var p3 = share(p2);
```

移动或者拷贝有所有权指针的结构:
```
struct A {
    var i: own* Int;
    fun copy(): A { ... }
}
var a: A;
var x = a; //compile error
var b = move a;
var c = a.copy();
```

### Unsafe
在unsafe块中解引用裸指针

```
var p: raw* Int;
...
unsafe {
    var i = *p;
}
```

Unsafe 函数必须在unsafe块中调用
```
unsafe fun foo() { ... }

fun main() {
    unsafe {
        foo();
    }
}
```

### 继承

和Java一样的单继承
```
trait I {
    virtual fun foo();
}

virtual struct B {
    var a: Int;
    fun bar() { ... }
}

struct A : B, I {
    override fun foo(B* b) {
        ...
    }
}

```

### With块

with块不是C++的命名初始化， 它可以包含任何语句.
```
struct A {
    var i: Int;
    fun init() { ... }
}

var a  = A { .init(); .i = 0; };
var a: own* A = alloc$<A>() { .i = 0; };
```


### 指针用法

总是通过`.`来访问
```
var a: A;
var b: own* A;
a.foo();
b.foo();
```

### 类型转换和判断:
```
var a = p as own* A;
var b = p is own* A;
```

### 数组

静态大小的数组
```
var a  = []Int { 1,2,3 };
var a: [15]Int;
```


### 泛型类型
泛型类型通过'$<'开头
```
struct Bar$<T> {
    fun foo() {
        ...
    }
}

T fun foo$<T>(a: T) {
    return a;
}

var b: Bar$<Int>;
```

### Non-nullable

指针默认是不可以为null的
```
var a: own*? B;
var b: own* B = a!;
```
通过感叹号把可空转为不可空。

### 不可变性

和C++类似
```
var p : raw* const Int;
var p : const raw* Int;
var p : const raw* const Int;
```


### 可见性
```
public
private
protected
readonly
```
'readonly' 的意思是公开读，私有写。

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

### 模块

模块是命名空间，也是编译单元和部署单元。

一个模块包含好多源文件和文件夹.

模块通过构建脚本来定义:
```
name = std
summary = standard library
outType = lib
version = 1.0
depends = sys 1.0
srcDirs = src/
```

在代码里面导入外部模块:
```
import std::*;
import std::Vec;
```

### 闭包

```
fun foo(f: fun(a:Int) ) {
    f(1);
}

foo(fun(a:Int){ ... });
```

### 类型别名

别名:
```
typealias VecInt = std::Vec$<Int>;
```

### 枚举

```
enum Color {
    red = 1, green, blue
}

var c = Color::red;
```

### 默认参数和命名参数

```
fun foo(a: Int, b: Int = 0) {
}

fun main() {
    foo(a : 10);
}
```
