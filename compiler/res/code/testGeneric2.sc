abstract struct LT {
    var next: own*? LT;
}

struct L$<T: LT> {
    fun get(): *? T {
        return null;
    }
}

private struct ME$<K> {
    var k: K;
    var next: own*? ME$<K>;
}

private struct MEL$<K> {
    var list: L$<ME$<K>>;

    fun foo(a: K) {
        list.get().k;
    }
}