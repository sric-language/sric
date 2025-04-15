

## Install

Require:
- JDK 17+
- C++ compier that support C++17
- fanx/fmake (Options)

Build:
1. Add "bin" into your env path
2. run build.sh

## IDE

1. Search 'sric-language' in vscode marketplace, install it.
2. Configure sricHome to point to the sric directory (the parent directory of bin).


## Usage

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
