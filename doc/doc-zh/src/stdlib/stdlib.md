

## 标准库

### 概述
- sric: 自带标准库
- jsonc: JSON解析和压缩库
- serial： 序列化库，基于动态反射功能。
- sricNet: 网络库，支持native和webassembly

[标准库文档](/apidoc.html)

### 使用C语言库

cstd模块只导出了一部分常用的C函数，期待您的补充。

如果使用到没有导出的C语言库，可自行导出。例如:

```
externc fun printf(format: raw* const Int8, args: ...)

fun main() {
    printf("Hello World\n")
}
```

使用externc声明即可。宏按照const变量来声明。
更多请参见[C++交互](cpp.md)。


### String
在Sric中字符串是raw* const Int8类型的，可以自动转为String类型。
```
var str: String = "abc"
```
有些情况下可能需要手动转String:
```
var str = asStr("abc")
```

### DArray

DArray类似于C++的std::vector。用于存储需要动态增长大小的数据。

```
var a : DArray$<Int>
a.add(1)
a.add(2)

verify(a[0] == 1)
```

### HashMap

HashMap用来存储key-value数据。
```
var map = HashMap$<Int, String>{}
map.set(1, "1")
map.set(2, "2")

verify(map[2] == "2")
```


### 读写文件
可用FileStream来读写文件。

写入文件：
```
var stream = FileStream::open("tmp.txt", "wb")
stream.writeStr("Hello\nWorld")
```

读取文件:
```
var stream = FileStream::open("tmp.txt", "rb")
var line = stream.readAllStr()
```
第二个参数的含义，参见C语言的fopen函数。
