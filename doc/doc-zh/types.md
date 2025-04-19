

### 指针
- 指针分为所有权指针、非所有权指针、裸指针。
- 和C/C++不同的是，指针的＊号放在类型的前方。

```
var p: Int             //值类型
var p: own* Int;       //所有权指针
var p: * Int;          //非所有权指针
var p: & Int;          //引用
var p: raw* Int;       //裸指针
```

#### 所有权指针
- 所有权指针表示拥有所指对象，在作用域消失时需要释放对象。
- 传递或者赋值所有权指针时需要显式移动或者拷贝。
```
var p1: own* Int = ...;
var p2 = move p1;
var p3 = share(p1);
```

#### 非所有权指针

- 非所有权指针使用没有限制，不会像Rust一样有很多限制。
- 程序会在运行时检查所引用的对象是否有效。
- 所有权指针可以自动转换成非所有权指针和裸指针。
```
var p1: own* Int = ...;
var p4: * Int = p1;
var p5: raw* Int = p1;
```

#### 裸指针
裸指针是C/C++的指针，需要在安全模式中使用。

```
var p: raw* Int = ...;
unsafe {
    var i = *p;
}
```

本地字段取地址后为非所有权指针(数组除外）。
```
var i: Int = 0;
var p: *Int = &i;
```

其他类型的指针可以自动转为裸指针类型。

#### 指针运算
只有裸指针才能进行指针运算。指针运算只能在unsafe块，或者unsafe函数中进行。


### 引用
引用的概念同C++，但在sric中引用只能用在函数参数和返回值中。
```
fun foo(a: & const Int) {
}
```

### 数组
数组特指静态大小数组。如果需要动态数组，请参考标准库中的DArray。
数组的括号写在类型前面
```
var a: [5]Int;
```
数组初始化时可以省略数组大小，由编译器自动推断
```
var a = []Int { 1,3,4 };
```
为了能通过变量指定数组大小，特意增加了constexpr关键字。
```
constexpr var size: const Int = 15;
var a: [size]Int;
```
目前constexpr只能赋值字面量，远没有C++的constexpr强大，待改进。

### Non-nullable

指针默认是不可以为null的，除非特殊声明。
```
var a: own*? B = null;
var a: own* B = null; //compile error
```
在可空指针转为不可空时，编译器会自动插入空指针检查。

### 不可变性

和C++类似，const可以修饰指针本身，也可以修饰指针所指对象。
```
var p : raw* const Int;
var p : const raw* Int;
var p : const raw* const Int;
```
