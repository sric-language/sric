

## 安装

需要
- JDK 17+
- C++ 编译器 支持C++17

1. 添加 "bin" 到你的环境变量
2. 运行 build.sh

## 用法

```
sric test.scm
```

生成的 C++ 代码位置在 "sric/output" 目录。


## 通过fmake来编译 (可选的)
```
fan fmake output/test.fmake -debug
```
