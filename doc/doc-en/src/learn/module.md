## Modularization

In Sric, modules serve as namespaces, compilation units, and deployment units. Software consists of multiple interdependent modules.

### Module Definition
Modules are defined through build scripts with `.scm` extension:
```
name = hello
summary = hello
outType = exe
version = 1.0
depends = sric 1.0
srcDirs = ./
```
The source directory srcDirs must end with `/.` The compiler will automatically search all .sric files in the directory.

### Module Import
Import external modules in code:

```
import sric::*;
import sric::DArray;
```
Where `*` imports all symbols under the module. Imported modules must be declared in the depends field of the build script.
