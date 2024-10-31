import sric::*;

fun testHashMap() {
    var map = HashMap$<String, Int>{ .defValue = 0; .init(); };
    map.set(asStr("1"), 1);
    map.set(asStr("2"), 2);

    var i =  map.get(asStr("1"));
    printf("%d\n", i);
}