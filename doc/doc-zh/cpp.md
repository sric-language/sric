
## 和C++交互
sric可以很容易的和C++交互。sric编译为人类可读的C++代码，可以像C++代码一样被C++直接调用。

调用C++代码，只需要将C++代码的原型声明一下，即可调用。C语言/无命名空间代码使用externc来修饰；同名命名空间使用extern来修饰；其他情况使用符号映射。

### C语言/无命名空间
```
externc fun printf(format: raw* const Int8, args: ...);

fun main() {
    printf("Hello World\n");
}
```

### 同名命名空间
当C++命名空间和模块名称相同时。
C++:
```
namespace xx {
    class P {
        void foo();
    };
}
```
Sric:
```
//xx module
extern struct P {
    fun foo();
}
```
这种情况下Sric代码的模块名称必须和C++的命名空间一致。

### 符号映射
也可以用symbol注解来映射符号名称，例如。
C++:
```
namespace test {
    void hi() {
    }
}

```
Sric:
```
//@extern symbol: test::hi
extern fun hello();
```

此时在Sric中调用hello将调用C++的hi方法。

### 包含头文件
在顶级声明前面用@#include注解来包含特殊的C++头文件

```
//@#include "test.h"
```

### 有参构造函数

由于Sric不支持有参数的构造函数，所以使用makePtr,makeValue来调用有参数的构造函数。

### 完整示例

```
import sric::*;


//@#include <vector>
//@extern symbol: std::vector
extern struct vector$<T> {
    fun size(): Int;
}

fun testExtern() {
    var v = makePtr$<vector$<Int>>(3);
    verify(v.size() == 3);
}

fun testExtern2() {
    var v2 = makeValue$<vector$<Int>>(3);
    verify(v2.size() == 3);
}
```

## 从C++头文件生成Sric接口
使用tool目录的python脚本，可以由C++头文件生成的sric原型。


## 不使用fmake进行编译
可以自己编译生成的C++代码，位于sric/output目录下。

可以定义SC_NO_CHECK和SC_CHECK宏。
- SC_CHECK表示进行安全检查。
- SC_NO_CHECK表示不进行安全检查。

当没有这两个宏时，按照_DEBUG和NDEBUG宏来自动定义。

## Sric代码和C++代码混合编译

在module.scm中增加fmake的配置项，以fmake.前缀开头，例如：
```
fmake.srcDirs = ./
fmake.incDirs = ./
```
