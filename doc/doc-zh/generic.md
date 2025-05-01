### 泛型
泛型和C++不同的是使用'$<'开头，这是为了消除泛型参数和小于运算符的歧义。
```
struct Tree$<T> {
}
```
泛型参数可以有示例类型，编译时以示例类型来做类型检查。
```
private struct Compareable$<T> {
    operator fun compare(t: *T): Int;
}
strcut Tree$<T : Compareable> {
}
```
泛型模板实例化时，可以传入任意满足示例类型的类型。不需要继承示例类型。
```
var tree = Tree$<Int> {};
```
