## 模板

### 模板定义
模板和C++不同的是使用`$<`开头，这是为了消除泛型参数和小于运算符的歧义。
```
struct Tree$<T> {
}
```
模板参数可以有示例类型，编译时以示例类型来做类型检查。例如:
```
abstract struct Linkable$<T> {
    var next: own*? T
    var previous: *? T
}

struct LinkedList$<T: Linkable$<T>> {
}
```

### 模板实例化
泛型模板实例化时，可以传入任意满足示例类型的类型。
```
var tree = Tree$<Int> {}
```
