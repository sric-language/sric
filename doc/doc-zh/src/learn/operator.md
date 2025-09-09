
### 操作符重载

使用`operator`关键字来实现操作符重载。

```
struct A {
    operator fun mult(a: A): A { ... }
}

var c = a * b;
```

可重载的操作符:
```
methods    symbol
------     ------
plus       a + b 
minus      a - b 
mult       a * b 
div        a / b 
get        a[b] 
set        a[b] = c
compare    == != < > <= >=
add        a,b,c;
```

### 逗号运算符

逗号运算符只在with块内有效.
```
x { a, b, c }
```
等价于
```
x { .add(a).add(b).add(c); }
```
