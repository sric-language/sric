

# 安装

### 1.安装需要的软件
- JDK 17+
- C++ 编译器(支持C++17)
- [fanx](https://github.com/fanx-dev/fanx/releases)
- CMake

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
./vsvars.sh
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

# 用法

```
sric test.scm
```

其中.scm文件是模块定义文件，也是构建脚本。
生成的 C++ 代码位置在 "sric/output" 目录。

## 使用fmake构建
```
sric test.scm -fmake -debug
```

也可以单独手动运行fmake:
```
fan fmake output/test.fmake -debug
```
