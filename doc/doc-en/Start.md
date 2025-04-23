

# Install

### 1.Required
- JDK 17+
- C++ compiler (supporting C++17)
- [fanx](https://github.com/fanx-dev/fanx/releases)
- CMake
- git

Install the above software and configure the environment variables to ensure that commands such as java, jar, fan, and cmake are available in gitbash.

### 2.Build fmake
```
git clone git@github.com:chunquedong/fmake.git
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
git clone git@github.com:chunquedong/jsonc.git
cd jsonc
./build.sh
```

### 6.Build Sric
```
git clone git@github.com:sric-language/sric.git
cd sric
./build.sh
```
Add sric/bin to your PATH (restart gitbash afterward).


# IDE

1. Search 'sric-language' in vscode marketplace, install it.
2. Configure sricHome to point to the sric directory (the parent directory of bin).

# Hello World

1. Create an empty folder as the workspace

2. Create a file named main.sric with the following content:
```
import cstd::*;

fun main(): Int {
    printf("Hello World\n");
    return 0;
}
```
3. Create a module.scm file with the following content:
```
name = hello  
summary = hello  
outType = exe  
version = 1.0  
depends = sric 1.0, cstd 1.0  
srcDirs = ./
```
4. Compile
```
sric module.scm -fmake
```
5. Run
After successful compilation, the output executable filename will be printed. Execute it by enclosing it in quotes. For example:
```
'C:\Users\user\fmakeRepo\msvc\hello-1.0-release\bin\hello'
```

# Usage

```
sric test.scm
```

The .scm file is module build script.

The generated C++ code is located in the "sric/output" directory.


## Build by fmake
```
sric test.scm -fmake -debug
```
Or:
```
fan fmake output/test.fmake -debug
```
