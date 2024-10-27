
abstract struct Linkable {
    var next: own*? Linkable;
    var previous: *? Linkable;
}

struct LinkedList$<T: Linkable> {
    private var head: own*? T = null;
    private var tail: *? T = null;
    private var length: Int = 0;

    fun clear() mut {
        head = null;
        tail = null;
        length = 0;
    }

    fun size(): Int {
        return length;
    }

    fun add(elem: own* T) {
        if (tail == null) {
            insert(move elem);
            return;
        }

        elem.next = null;
        elem.previous = tail;
        tail.next = move elem;
        tail = tail.next;
        ++length;
    }

    fun insert(elem: own* T) {
        elem.next = move head;
        elem.previous = null;

        if (elem.next != null) {
            elem.next.previous = elem;
        }
        head = move elem;
        if (tail == null) {
            tail = head;
        }
        ++length;
    }

    fun insertBefore(elem: own* T, pos: *T) {
        if (pos.previous == null) {
            insert(elem);
            return;
        }
        elem.next = move pos.previous.next;
        elem.previous = pos.previous;
        pos.previous = elem;
        elem.previous.next = move elem;
        ++length;
    }

    fun remove(elem: *? T): Bool {
        if (elem == null) return false;
        if (elem.previous != null) {
            elem.previous.next = move elem.next;
        }
        if (elem.next != null) {
            elem.next.previous = elem.previous;
        }
        if (head == elem) {
            head = move elem.next;
        }
        if (tail == elem) {
            tail = elem.previous;
        }
        elem.next = null;
        elem.previous = null;
        --length;
        return true;
    }

    fun isEmpty(): Bool {
        return length == 0;
    }

    fun first(): *T {
        return head;
    }

    fun last(): *T {
        return tail;
    }

    fun end(): *T {
        return null;
    }
}