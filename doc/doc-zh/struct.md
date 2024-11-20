
## 结构
类没有构造函数，需要调用者自己初始化。
```
struct Point {
    var x: Int = 0;
    var y: Int;
}
var p = Point { .y = 1; };
```
初始化的语法叫做with块，不同于C的命名初始化。with块可以保护任何语句，并且可以用在非初始化场景。例如
```
var point = Point { .y = 1; };
point {
    .x = 2; if (a) { .y = 3; }
}
```

也可以提供一个初始化的方法，约定名称为init。
```
struct Point {
    var x: Int = 0;
    var y: Int;
    fun init(a: Int) {
        this { .y = a; }
    }
}
var p = Point { .init(1); };
```

## 方法
- 类型可以由方法，非静态成员函数，有一个隐藏的this指针。
- 方法必须出现在数据成员之后。
- this的可变性修饰在函数后部，返回类型之前，例如:
```
struct Point {
    fun length() const : Float {
        ...
    }
}
```

struct可以包含静态函数，但不能保护静态字段。静态函数没有隐式的this指针。
```
strcut Point {
    static fun foo() {
    }
}
```

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
并且可以可选的设置为一个整数值。

## 联合体
sric暂不支持union。可以使用强制类型转换来替代。
```
struct U {
    var data: [8]Int8;
}

var i = *((&u.data) as raw*Int);
```
或者使用继承多态来满足需求。
也许将来会加入像C++一样的std::variant作为替代。

## 继承
- 不支持多继承，类似于Java
- 被继承的类需要标记为virtual或者abstract
- Trait相对与Java的interface，不能有数据成员和方法实现。
- 继承的分号后面必须先写类（如果有）再写Trait。
- 重写父类的virutal或者abstract方法时，需要加override标记。
- 使用super关键字调用父类方法。
```
virtual strcut B {
    virtual fun foo() {}
}
trait I {
    abstract fun foo2();
}
struct A : B , I {
    override fun foo2() {}
}
```