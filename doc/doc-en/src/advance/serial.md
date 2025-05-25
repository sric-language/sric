## Serialization
Sric supports serialization through built-in dynamic reflection, using the [HiML](https://github.com/chunquedong/jsonc) text format.

### Serialization Example
Classes to be serialized require the `reflect` marker:
```sric
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
For non-polymorphic objects like Point, the type name "testSerial::Point" must be explicitly provided.

### Skipping Serialization
Use Transient annotation to exclude fields:

```sric
reflect struct Point {
    var x: Int;
    //@Transient
    var y: Float;
}
```
### Post-Deserialization Handling
The _onDeserialize method is automatically called after deserialization:

```sric
reflect struct Point {
    var x: Int;
    fun _onDeserialize() {
    }
}
```
### Simple Serialization Mode
Normally custom classes serialize as HiML objects. The `SimpleSerial` annotation enables string serialization (e.g. "1 2 3 4" instead of structured format`Insets{top=1,right=2,bottom=3,left=4}`):

```sric
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
Serialization/deserialization automatically calls `toString` and `fromString` - these methods must exactly match the shown signatures.