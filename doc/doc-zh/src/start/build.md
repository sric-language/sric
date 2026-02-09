
# 安装

### 1.安装需要的软件
- [JDK 17+](https://www.oracle.com/java/technologies/downloads/)
- 支持C++20的C++ 编译器: gcc 11+、 clang 17+、 Xcode 16+、 Visual Studio 2022+
- [CMake](https://cmake.org/download/)
- [git](https://git-scm.com/downloads)
- [VSCode](https://code.visualstudio.com/)

安装以上软件，并配置环境变量，确保java、jar、cmake等命令在git bash中可用。

### 2.构建fmake
```
git clone https://github.com/chunquedong/fmake.git
cd fmake
sh build.sh
```
并将fmake/bin目录加入到环境变量PATH中

在Windows系统上使用微软C++编译器工具集:
```
cd fmake
source vsvars.sh
cd -
```

[更多关于fmake的信息](https://github.com/chunquedong/fmake)

### 3.构建jsonc
```
git clone https://github.com/chunquedong/jsonc.git
cd jsonc
sh build.sh
```

### 4.构建Sric

```
git clone https://github.com/sric-language/sric.git
cd sric
chmod a+x bin/sric
sh build.sh
sh build_debug.sh
```

添加sric的"bin"目录到你的环境变量（配置完环境变量需要重启git bash）
