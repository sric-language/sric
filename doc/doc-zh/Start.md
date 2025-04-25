

# 安装

### 1.安装需要的软件
- JDK 17+
- C++ 编译器(支持C++17)
- [fanx](https://github.com/fanx-dev/fanx/releases)
- CMake
- git

安装以上软件，并配置环境变量，确保java、jar、fan、cmake等命令在gitbash中可用。

### 2.构建fmake
```
git clone git@github.com:chunquedong/fmake.git
cd fmake
fanb pod.props
```

在Windows系统上使用微软C++编译器工具集:
```
cd fmake
source vsvars.sh
cd -
```

[更多关于fmake的信息](https://github.com/chunquedong/fmake)

### 3.构建jsonc
```
git clone git@github.com:chunquedong/jsonc.git
cd jsonc
./build.sh
```

### 4.构建Sric
```
git clone git@github.com:sric-language/sric.git
cd sric
./build.sh
```
添加sric的"bin"目录到你的环境变量（配置完环境变量需要重启gitbash）

# IDE

1. 在vscode插件市场中搜索'sric-language',并安装。
2. 在插件的设置页配置sricHome指向sric目录(bin的上一级)。

# Hello World

1. 创建一个空文件夹作为工作空间
2. 创建文件main.sric，内容如下:
```
import cstd::*;

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
depends = sric 1.0, cstd 1.0
srcDirs = ./
```

4. 编译
```
sric module.scm -fmake
```

5. 运行
编译成功后会打印输出的可执行文件名，加上引号来执行。例如:
```
'C:\Users\user\fmakeRepo\msvc\hello-1.0-release\bin\hello'
```


其中.scm文件是模块定义文件，也是构建脚本。
生成的 C++ 代码位置在 "sric/output" 目录。



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