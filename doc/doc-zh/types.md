

### 指针
- 指针分为所有权指针、非所有权指针、裸指针。
- 和C/C++不同的是，指针的＊号放在类型的前方。

#### 所有权指针
- 所有权指针表示拥有所指对象，在作用域消失时需要释放对象。
- 传递或者赋值所有权指针时需要显式移动或者拷贝。
```
var p1: own* Int = ...;
var p2 = move p1;
var p3 = share(p1);
```
经测试sric的所有权指针性能好于shared_ptr和unique_ptr。一般系统中所有权指针数量不会太多，所以不用担心性能问题。

#### 非所有权指针

- 非所有权指针使用没有限制，不会像Rust一样有很多限制。
- 程序会在运行时检查所引用的对象是否有效。
- 所有权指针可以自动转换成非所有权指针和裸指针。
```
var p1: own* Int = ...;
var p4: * Int = p1;
var p5: raw* Int = p1;
```
非所有权指针性能和裸指针几乎一样。

#### 裸指针
裸指针是C/C++的指针，需要在安全模式中使用。

```
var p: raw* Int = ...;
unsafe {
    var i = *p;
}
```

增加了refalbe修饰的字段取地址后为非所有权指针，负责为裸指针。
```
var i: refable Int = 0;
var p: *Int = &i;
```
为了方便在函数调用时，取地址的操作可自动转为非所有权指针。例如
```
fun foo(p: *Int) {};
var i = 0;
foo(&i);  //auto convert raw*Int to *Int
```
所以，推荐函数参数尽可能用非所有权指针代替裸指针。

其他类型的指针可以自动转为裸指针类型。

#### 指针运算
只有裸指针才能进行指针运算。指针运算只能在unsafe块，或者unsafe函数中进行。


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
constexpr var size = 15;
var a: [size]Int;
```
目前constexpr只能赋值字面量，远没有C++的constexpr强大，待改进。

### Non-nullable

指针默认是不可以为null的，除非特殊声明。
```
var a: own*? B = null;
var a: own* B = null; //compile error
```
通过感叹号把可空转为不可空。
```
var b: own* B = a!;
```

### 不可变性

和C++类似，const可以修饰指针本身，也可以修饰指针所指对象。
```
var p : raw* const Int;
var p : const raw* Int;
var p : const raw* const Int;
```

函数参数默认是const的，可以通过mut关键字声明为可变类型。
```
struct Bar {
    var i: Int = 0;
    fun set(a: mut Int) const : Int {
        i = a;
    }
}
```
