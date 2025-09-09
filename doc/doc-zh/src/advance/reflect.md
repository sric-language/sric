


### 反射
默认不启用反射，需要手动添加reflect标记。
```
reflect struct Point {
    var x: Int
    var y: Int
}
```
注解可以在反射API中获取
```
//@SimpleSerial
reflect struct Point {
    var x: Int
    var y: Int
}
```
