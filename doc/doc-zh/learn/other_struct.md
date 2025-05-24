

## 类型别名
类型别名相当于C的typedef
```
typealias size_t = Int32;
```


## 枚举
枚举和C++相同，但总是占命名空间。
```
enum Color {
    Red, Green = 2, Blue
}

fun foo(c: Color) {}

foo(Color::Red);
```
可以设置大小:
```
enum Color : UInt8 {
    Red, Green = 2, Blue
}
```

## 联合体
sric暂不支持union。可使用继承多态来满足需求。


## 不安全结构

unsafe结构完全和对应的C++类一致，不包含安全检查需要的标记位。extern结构默认是unsafe的。

unsafe里的this的类型是裸指针，而不是安全指针。如果对象是独立用new关键字分配的，可以通过rawToRef转为安全指针。
```
unsafe struct A {
    fun foo() {
        var self = rawToRef(this);
    }
}
```
