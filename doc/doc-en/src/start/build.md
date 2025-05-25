
# Install

### 1.Required
- [JDK 17+](https://www.oracle.com/java/technologies/downloads/)
- C++ compiler (supporting C++20): gcc 11+, clang 17+, Xcode 16+, Visual Studio 2022+
- [Fanx](https://github.com/fanx-dev/fanx/releases)
- [CMake](https://cmake.org/download/)
- [git](https://git-scm.com/downloads)
- [VSCode](https://code.visualstudio.com/)

Install the above software and configure the environment variables to ensure that commands such as java, jar, fan, and cmake are available in git bash.

### 2.Build fmake
```
git clone https://github.com/chunquedong/fmake.git
cd fmake
fanb pod.props
```

Use the Microsoft C++ compiler toolchain on Windows:
```
cd fmake
source vsvars.sh
cd -
```
[About fmake](https://github.com/chunquedong/fmake)

### 3.Build jsonc
```
git clone https://github.com/chunquedong/jsonc.git
cd jsonc
sh build.sh
```

### 6.Build Sric
```
git clone https://github.com/sric-language/sric.git
cd sric
chmod a+x bin/sric
sh build.sh
sh build_debug.sh
```
Add sric/bin to your PATH (restart git bash afterward).
