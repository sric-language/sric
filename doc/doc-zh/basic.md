### 内建类型
- Int, Int8, Int16, Int32, Int64, UInt8, UInt16, UInt32, UInt64
- Float, Float32, Float64
- Bool
- Void

Int默认32位，Float默认64位。

### 字符串
字符串可以多行
```
var s = "ab
         cd";
```
三引号字符串，密码的双引号不需要转义。
```
var s = """ab"
           cd""";
```
字符串字面量的类型是raw*const Int8, 可以自动转为String

### 注释
普通注释
```
//comment
/*
 comment
*/
```
文档注释
```
/**
    introduce
*/
```
命令注释
```
//@method: GET
```
命令相当与Java的注解，可以通过反射接口动态反射。

### 变量定义
- 使用var定义变量，不管是否可变都用var。
- 变量类型写变量后。
- 每个语句只能定义一个变量。
```
var i: Int = 0;
```

只有函数内的局部变量才支持类型推断
```
var i = 0;
```

非成员变量需要有初始化值，如果想保持随机值，则使用uninit关键字。
```
var i = uninit;
```

全局变量必须是不可变的，除非加unsafe修饰
```
var i: const Int = 0;
```



### 函数定义
- 函数使用fun开始
- 返回值是Void时，可省略返回值
- 函数的名称必须是唯一值的，不支持通过参数重载
- 函数的参数默认是不可变的
```
fun foo(a: Int): Int { return 0; }
fun foo2() {}
```
默认参数和命名参数
```
fun foo(a: Int, b: Int = 0) {}
foo(a: 1);
```
命名参数让你能显式写出参数名称，增加可读性。

### 前向声明
没有类似于C/C++的前向声明，前面的函数也能调用后面的。
因为sric采用的是多层编译的编译器架构。

### 可见性
变量和函数都支持可见性标记
```
public
private
protected
readonly
```
例如
```
private fun foo() {}
readonly var i: Int = 0;
```
- 全局变量和函数的可见性保护区域是当前文件，如果声明为private则外部文件不可见。
- 默认的可见性都是public，所有不用写任何public。
- protected表示当前模块内可见，或者继承的子类可见。
- readonly只能用来修饰变量，不能修饰函数。表示公开读，私有写。
