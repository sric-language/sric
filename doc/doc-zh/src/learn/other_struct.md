

## 类型别名
类型别名相当于C的typedef
```
typealias size_t = Int32
```


## 枚举
枚举和C++相同，但总是占命名空间。
```
enum Color {
    Red, Green = 2, Blue
}

fun foo(c: Color) {}

foo(Color::Red)
```
可以设置大小:
```
enum Color : UInt8 {
    Red, Green = 2, Blue
}
```


## 不安全结构

unsafe结构完全和对应的C++类一致，不包含安全检查需要的标记位。extern结构默认是unsafe的。

unsafe里的this的类型是裸指针，而不是安全指针。如果对象是独立用new关键字分配的，可以通过rawToRef转为安全指针。
```
unsafe struct A {
    fun foo() {
        var self = rawToRef(this)
    }
}
```

## dconst方法

为了减少代码重复，以及函数重载这种复杂特性，Sric提供了dconst方法。

```
struct A {
    var i: String
    fun foo() dconst : * String {
        ...
        return &i
    }
}
```

dconst方法在编译器内部会自动生成cosnt和非const两个版本的。等价于下面的C++代码:

```
class A {
    string i;
public:
    string* foo() {
        ...
        return &i;
    }

    const string* foo() const {
        ...
        return &i;
    }
};
```
