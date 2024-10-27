trait Hashable {
    abstract fun hash(): Int;
    abstract operator fun compare(p: *Hashable): Int;
}

struct HashMap$<K:Hashable, V> {

}