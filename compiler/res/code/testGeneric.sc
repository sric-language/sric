
trait NT {
    abstract operator fun plus(that: ref* NT): ref* NT;
}

struct A$<T : NT> {
    var i: T ;
    fun foo() mut : raw* T {
       return &i;
    }

    fun foo2(): T {
        return *(i + &i);
    }
}

fun gf$<T>(v: T): T {
    return v;
}

fun main()
{
    var a = A$<Int>{};
    a.i = 2;
    var b : raw* Int = a.foo();

    var i: Int = gf$<Int>(1);
}
