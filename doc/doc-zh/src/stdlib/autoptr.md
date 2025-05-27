# 智能指针

Sric提高了类似与C++的一组智能指针。包括SharedPtr、WeakPtr等。


## SharedPtr

SharedPtr是自动引用计数的，有一定的开销。可从现有的own*获取。例如:

```
var p = new Int;
var sp: SharedPtr$<Int> = toShared(p);
```

SharedPtr可通own*相互转换:
```
var p = sharedPtr.getOwn();
sharedPtr.set(p);
```

## WeakPtr

使用own*和SharedPtr都可能产生循环引用，导致内存泄漏。可使用WeakPtr来打破循环引用。

```
var p = new Int;
var wp: WeakPtr$<Int> = toWeak(p);
```
使用时通过lock方法转化为own*，然后进行使用。
```
var sp : own*? Int = wp.lock();
```
如果WeakPtr引用的对象已经被释放，则lock方法返回null。
