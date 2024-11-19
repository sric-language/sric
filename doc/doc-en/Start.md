

## Install

Require:
- JDK 17+
- C++ compier that support C++17
- Maven

Build:
1. Add "bin" into your env path
2. run build.sh

## IDE

1. Search 'sric-language' in vscode marketplace, install it.
2. Config Language Server Path, Language Server Arguments in plugin setting.


## Usage

```
sric test.scm
```

The generated C++ code is located in the "sric/output" directory.


## Compile by fmake (Option)
```
fan fmake output/test.fmake -debug
```
