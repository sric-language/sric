
## 结构
类没有构造函数，需要调用者自己初始化。
```
struct Point {
    var x: Int = 0;
    var y: Int = uninit;
    var z: Int;
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
    var x: Int;
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

Point::foo();
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

var i = unsafeCast$<raw*Int>(&u.data);

```
或者使用继承多态来满足需求。
也许将来会加入像C++一样的std::variant作为替代。

## 继承
- 不支持多继承，类似于Java
- 被继承的类需要标记为virtual或者abstract
- Trait相当与Java的interface，不能有数据成员和方法实现。
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

## 构造函数和析构函数
Sric没有C++类似的构造函数，只有默认构造函数，不能有参数。例如
```
struct A {
    fun new() {
    }
    fun delete() {
    }
}
```
Sric绝大部分情况是不需要写析构函数的，因为所有权机制会自定清理内存。

构造函数是为了弥补原地初始化不能写复杂逻辑的问题。例如可以用三个点的语法，在构造函数中初始化。
```
struct A {
    var p : own* Int = ...;
    fun new() {
        p = new Int;
    }
}
```

## 不安全结构

unsafe结构完全和对应的C++类一致，不包含安全检查需要的标记位。extern结构默认是unsafe的。
unsafe里的this的类型是裸指针，而不是安全指针。如果对象是独立分配的，可以通过rawToRef转为安全指针。
```
unsafe struct A {
    fun foo() {
        var self = rawToRef(this);
    }
}
```
