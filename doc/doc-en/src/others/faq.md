# Frequently Asked Questions

### Unicode Character Encoding Issues
In Git Bash, run:
```
cmd "/c chcp 65001>nul"
```
In CMD, run:
```
chcp 65001>nul
```

### Compiler Doesn't Support C++20
Since coroutines require C++20, you can specify C++ version (note coroutine-related code will fail to compile):
```
sric module.scm -fmake -c++17
```
