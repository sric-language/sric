
abstract struct Linkable$<T> {
    var next: own*? T;
    var previous: *? T;
}

struct LinkedList$<T: Linkable$<T>> {
    private var head: own*? T = null;
    private var tail: *? T = null;
    private var length: Int = 0;

    fun clear() {
        head = null;
        tail = null;
        length = 0;
    }

    fun size() const : Int {
        return length;
    }

    fun add(elem: mut own* T) {
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

    fun insert(elem: mut own* T) {
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

    fun insertBefore(elem: mut own* T, pos: * mut T) {
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

    fun remove(elem: *? mut T) : Bool {
        if (elem == null) return false;
        
        if (elem.next != null) {
            elem.next.previous = elem.previous;
        }

        if (head == elem) {
            head = move elem.next;
        }
        if (elem.previous != null) {
            elem.previous.next = move elem.next;
        }

        if (tail == elem) {
            tail = elem.previous;
        }
        elem.next = null;
        elem.previous = null;
        --length;
        return true;
    }

    fun isEmpty() const : Bool {
        return length == 0;
    }

    fun first(): *?T {
        return head;
    }

    fun last(): *?T {
        return tail;
    }

    fun end(): *?T {
        return null;
    }
}