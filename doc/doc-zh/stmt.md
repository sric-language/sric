
## 语句
- 所有语句使用分号结尾。
- 不支持do while语句和goto语句，其他和C++一致。
- switch语句不支持自动贯穿，如果想贯穿需要用fallthrough关键字
```
switch (i) {
    case 1:
        fallthrough;
    case 2:
        printf("%d\n", i);
}
```

### Unsafe
在unsafe块中解引用裸指针

```
var p: *Int;
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

## 表达式
- 除了位运算的优先级外其他和C/C++相同，位运算优先级高于等于运算符。
```
if (i & Mask != 0) {}
//same as
if ((i & Mask) != 0) {}
```
- 只支持`++i`，不支持`i++`
- 类型转换和类型检查用as和is
```
if (a is String) ...
var b = a as String;
```

### With块

with块不是C++的命名初始化， 它可以包含任何语句.
```
struct A {
    var i: Int;
    fun init() { ... }
}

var a  = A { .init(); .i = 0; };
var a: own* A = new A { .i = 0; };
```

### 指针访问
指针也通过`.`来访问，不用`->`
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

### 未初始化变量
默认所有变量都是初始化的，原始类型自动初始化为零值。如果为了性能不想初始化可以用uninit关键字。
```
var a: Int = uninit;
```

