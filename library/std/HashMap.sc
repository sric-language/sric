trait Hashable {
    abstract fun hashCode(): Int;
    abstract operator fun compare(p: *Hashable): Int;
}

private struct MapEntry$<K:Hashable, V> {
    var k: K;
    var v: V;
    var next: own*? MapEntry$<K, V>;
    var previous: *? MapEntry$<K, V>;
}

private struct MapEntryList$<K:Hashable, V> {
    var list: LinkedList$<MapEntry$<K,V>>;
}

struct HashMap$<K:Hashable, V> {
    private var table: DArray$<MapEntryList$<K,V>>;
    var defValue: V;
    private var length: Int = 0;

    fun init(size: Int = 2) {
        table.resize(2);
    }
    
    private fun getEntryList(k: K): *MapEntryList$<K,V> {
        var h = k.hashCode();
        h %= table.size();
        return table[h];
    }

    fun get(k: K): V {
        var list = getEntryList(k);
        for (var itr = list.list.first(); itr != null; itr = itr.next) {
            if (itr.k == &k) {
                return itr.v;
            }
        }
        return defValue;
    }
    
    fun set(k: K, v: V) mut : Bool {
        var list = getEntryList(k);
        for (var itr = list.list.first(); itr != null; itr = itr.next) {
            if (itr.k == &k) {
                itr.v = v;
                return false;
            }
        }
        var entry = alloc$<MapEntry$<K,V>>() { .k = k; .v = v; };
        list.list.add(move entry);
        ++length;
        return true;
    }
    
    fun size(): Int {
        return length;
    }
    
    fun remove(k: K) mut : Bool {
        var list = getEntryList(k);
        for (var itr = list.list.first(); itr != null; itr = itr.next) {
            if (itr.k == &k) {
                list.list.remove(itr);
                return true;
            }
        }
        return false;
    }
    
    fun contains(k: K): Bool {
        var list = getEntryList(k);
        for (var itr = list.list.first(); itr != null; itr = itr.next) {
            if (itr.k == &k) {
                return true;
            }
        }
        return false;
    }
    
    fun eachWhile(f: fun(v:V, k:K):Bool) {
        for (var i = 0; i<table.size(); ++i) {
            var list = table.get(i);
            for (var itr = list.list.first(); itr != null; itr = itr.next) {
                if (!f(itr.v, itr.k)) {
                    return;
                }
            }
        }
    }
}