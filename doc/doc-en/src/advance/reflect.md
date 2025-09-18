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

#### Usage
```
import sric::*
import serial::*
import waseGraphics

unsafe fun main(): Int {
    //force load reflect metadata
    var c : waseGraphics::Color
    c.init(1, 1, 1, 1);

    var rmodule = findModule("waseGraphics")
    printf("rmodule %p\n", rmodule as raw*? Void)

    //make instance
    var rtype = findRType("waseGraphics::Color")
    var obj = newInstance(*rtype)

    //access field
    var rgba = (unsafeCast$<raw*Int8>(obj) + rtype.fields[0].offset) as raw*UInt32
    *rgba = 0xff8845ff

    //call method
    var rmethod = findInstanceMethod(rtype, "toString")
    var str = callInstanceToString(rmethod.pointer, obj)
    printf("%s\n", str.c_str())
    return 0
}
```