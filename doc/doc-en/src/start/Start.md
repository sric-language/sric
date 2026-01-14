

# Install

[build from source](build.md)


# IDE

1. Search 'sric-language' in vscode marketplace, install it.
2. Configure sricHome to point to the sric directory (the parent directory of bin).

After configuring, restart VSCode. If features such as Go to Definition, Auto Completion, and Outline View are available, it means the configuration was successful. If you recompile the Sric source code, you need to close VSCode first.

# Hello World

1. Create an empty folder as the workspace

2. Create a file named main.sric with the following content:
```
import sric::*

fun main(): Int {
    printf("Hello World\n")
    return 0
}
```
3. Create a module.scm file with the following content:
```
name = hello  
summary = hello  
outType = exe  
version = 1.0  
depends = sric 1.0
srcDirs = ./
```
4. Build
```
sric module.scm -fmake
```
build debug mode:
```
sric module.scm -fmake -debug
```

Note: If you are using the Microsoft C++ compiler toolset on Windows, you need to run the following command every time you open a command terminal:
```
cd fmake
source vsvars.sh
cd -
```

5. Run

After compilation, the console will print the output file path. Run it with quotes. For example:

```
'C:\Users\xxx\fmakeRepo\msvc\test-1.0-debug\bin\test'
```

## Build by fmake

The build process without -fmake solely outputs C++ code (under "sric/output").
```
sric hello.scm
```

Then compile it separately by manually running fmake:
```
fan fmake output/hello.fmake -debug
```

## Debug

Debugging the generated C++ code is supported via IDE project generation.
```
fan fmake output/hello.fmake -debug -G
```
The generated project files are located in the build folder under the parent directory.