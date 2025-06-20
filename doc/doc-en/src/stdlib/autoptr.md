# Smart Pointers

Sric provides C++-style smart pointers including SharedPtr, and WeakPtr.


## SharedPtr
Reference-counted with some overhead. Can be created from existing own*:

```sric
var p = new Int;
var sp: SharedPtr$<Int> = toShared(p);
```
Convertible with own*:

```sric
var p = sharedPtr.getOwn();
sharedPtr.set(p);
```
## WeakPtr
Breaks circular references that could cause memory leaks with own*/SharedPtr:

```sric
var p = new Int;
var wp: WeakPtr$<Int> = toWeak(p);
```
Use via lock() which returns nullable own*:

```sric
var sp : own*? Int = wp.lock();
```
Returns null if referenced object was freed.
