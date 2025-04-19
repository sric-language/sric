## 模块化
每个语言对模块的定义都有点不太一样，sric中模块既是命名空间，也是编译单元，也是部署单元。

### 模块定义
模块通过构建脚本来定义，构建脚本以scm为扩展名:
```
name = demo
summary = demo library
outType = lib
version = 1.0
depends = sys 1.0
srcDirs = src/
```
源码目录srcDirs需要以`/`结尾，编译器会自动搜索目录下的所有.sric文件。

### 模块导入
在代码里面导入外部模块:
```
import sric::*;
import sric::DArray;
```
其中`*`表示导入模块下的所有符号。导入的模块必须在构建脚本的depends字段声明。
