

# 安装
[从源码构建](build.md)

# IDE

1. 在vscode插件市场中搜索'sric-language',并安装。
2. 在插件的设置页配置sricHome指向sric目录(bin的上一级)。

配置好后重启vscode。如果有跳转到定义、自动完成、大纲视图等功能，说明配置成功。如果重新编译sric源码，需要先关闭vscode。

# Hello World

1. 创建一个空文件夹作为工作空间
2. 创建文件main.sric，内容如下:
```
import sric::*;

fun main(): Int {
    printf("Hello World\n");
    return 0;
}

```

3. 创建module.scm文件，内容如下:
```
name = hello
summary = hello
outType = exe
version = 1.0
depends = sric 1.0
srcDirs = ./
```

4. 编译和运行
```
sric module.scm -fmake
```
构建debug版本:
```
sric module.scm -fmake -debug
```

1. 运行

编译后控制台会打印输出文件路径，加上引号来运行。例如:
```
'C:\Users\xxx\fmakeRepo\msvc\test-1.0-debug\bin\test'
```

## 使用fmake构建

不加-fmake构建后，只生成C++代码（位置在 "sric/output" 目录）。
```
sric hello.scm
```

然后再单独手动运行fmake编译:
```
fan fmake output/hello.fmake -debug
```

## 调试
可通过生成IDE项目来调试生成的C++代码。
```
fan fmake output/hello.fmake -debug -G
```
生成的项目文件在上层目录的build文件夹下。

