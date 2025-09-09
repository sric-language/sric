### Reflection
Reflection is disabled by default and requires explicit `reflect` marker:
```sric
reflect struct Point {
    var x: Int
    var y: Int
}
```
Annotations can be accessed through reflection API:

```sric
//@SimpleSerial
reflect struct Point {
    var x: Int
    var y: Int
}
```