

# Install

### 1.Required
- JDK 17+
- C++ compiler (supporting C++17)
- [fanx](https://github.com/fanx-dev/fanx/releases)
- CMake

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
./vsvars.sh
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
