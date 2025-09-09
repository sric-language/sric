

## 从C++到Sric
### 类型比较

| C++  | Sric  |
| ----- | ---- |
| int | Int |
| short | Int16 |
| int32_t | Int32 |
| unsigned int | UInt32 |
| int64_t | Int64 |
| uint64_t | UInt64 |
| float | Float32 |
| double | Float/Float64 |
| void | Void |
| char | Int8 |


### 定义
| C++  | Sric  |
| ----- | ---- |
| const char* str | var str: raw* Int8 |
| void foo(int i) {} | fun foo(i: Int) {} |
| char a[4] | var a: [4]Int8 |
| const int& a | var a: & const Int |

### 类型定义

C++
```
#include <math.h>

class Point {
public:
    int x
    int y
    double dis(const Point &t) const {
        int dx = t.x - x;
        int dy = t.y - y;
        return sqrt(dx*dx + dy*dy);
    }
};
```
Sric:
```
import sric::*;

struct Point {
    var x: Int
    var y: Int
    fun dis(t: & const Point) const: Float {
        var dx = t.x - x
        var dy = t.y - y
        return sqrt(dx*dx + dy*dy)
    }
}
```

## 特性比较

### 从C++移除的功能

- 没有函数重载
- 没有头文件
- 没有大对象的隐式拷贝
- 不能一句定义多个变量
- 没有嵌套类、嵌套函数
- 没有class, 只有struct
- 没有命名空间
- 没有宏
- 没有向前声明
- 没有static的三重意思
- 没有友元
- 没有多继承
- 没有虚继承和私有继承
- 没有i++，只有++i
- 没有switch语句自动贯穿
- 没有模板特化
- 没有各种各样的构造函数

### 比C++多的

- 简单和容易
- 内存安全
- 模块化（模块化在这里有不同的定义）
- With块
- 不可空指针
- 动态反射
- 命名参数

