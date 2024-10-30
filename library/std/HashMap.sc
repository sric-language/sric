trait Hashable {
    abstract fun hash(): Int;
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
    var defValue: V;
    
    fun get(k: K): V {
        for (var itr = list.first(); itr != null; itr = itr.next) {
            if (itr.k == k) {
                return itr.v;
            }
        }
        return defValue;
    }
    
    fun set(k:K, v:V): Bool {
        for (var itr = list.first(); itr != null; itr = itr.next) {
            if (itr.k == k) {
                itr.v = v;
                return false;
            }
        }
        var entry = alloc$<MapEntry$<K,V>>() { .k = k; .v = v; };
        list.add(entry);
        return true;
    }
    
    fun remove(k: K): Bool {
        for (var itr = list.first(); itr != null; itr = itr.next) {
            if (itr.k == k) {
                list.remove(itr);
                return true;
            }
        }
        return false;
    }
}

struct HashMap$<K:Hashable, V> {
    private var table: DArray$<MapEntryList$<K,V>>;
    var defValue: V;
    private var length: Int;
    
    private fun getEntryList(k: K): *MapEntryList$<K,V> {
        var h = k.hash();
        h %= table.size();
        return table[h];
    }

    fun get(k: K): *V {
        var list = getEntryList(k);
        return list.get(k);
    }
    
    fun set(k: K, v: V) {
        var list = getEntryList(k);
        if (list.set(k, v)) {
            ++length;
        }
    }
    
    fun size() {
        return length;
    }
    
    fun remove(): Bool {
        var list = getEntryList(k);
        return list.remove(k);
    }
    
    fun contains(): Bool {
        var list = getEntryList(k);
        return list.get(k) != null;
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