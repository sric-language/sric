
## 序列化
sric通过内建的动态反射来支持序列化，序列化格式为文本格式[HiML](https://github.com/chunquedong/jsonc)。


### 序列化例子
要序列化的类需要reflect标记。例如：

```
reflect struct Point {
    var x: Int;
    var y: Float;
}

unsafe fun testSimple() {
    var encoder: Encoder;
    var obj = new Point { .x = 1; .y = 2; };
    var t = obj as *Void;
    var res = encoder.encode(t, "testSerial::Point");
    printf("%s\n", res.c_str());

    var decoder: Decoder;
    var p = decoder.decode(res);
    var obj2: raw* Point = unsafeCast$<raw*Point>(p);
    
    verify(obj2.x == obj.x);
    verify(obj2.y == obj.y);
}
```
上面的例子中由于Point是非多态对象，所以要显式传入名称"testSerial::Point"。

### 避免序列化
有时候有些字段不想序列化，可以用Transient注解来标记

```
reflect struct Point {
    var x: Int;

    //@Transient
    var y: Float;
}
```

### 反序列化后处理
有时候希望反序列化后，调用指定函数来恢复状态。名称为_onDeserialize的函数将被自动调用。
```
reflect struct Point {
    var x: Int;

    fun _onDeserialize() {
    }
}
```

### 简单模式序列化
正常情况下自定义类被序列化为HiML对象，可通过SimpleSerial注解将其序列化为字符串。例如我们想要把Insets序列化为"1 2 3 4"而不是"Insets{top=1,right=2,bottom=3,left=4}"

```
//@SimpleSerial
reflect struct Insets {
    var top: Int = 0;
    var right: Int = 0;
    var bottom: Int = 0;
    var left: Int = 0;

    fun toString() : String {
        return String::format("%d %d %d %d", top, right, bottom, left);
    }

    fun fromString(str: String): Bool {
        var fs = str.split(" ");
        if (fs.size() == 4) {
            top = fs[0].toInt32();
            right = fs[1].toInt32();
            bottom = fs[2].toInt32();
            left = fs[3].toInt32();
            return true;
        }
        return false;
    }
}
```

序列化和反序列化过程中会自动调用toString和fromString，两个函数的签名必须和上面完全一致。
