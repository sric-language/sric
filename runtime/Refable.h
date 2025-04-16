#ifndef _SRIC_REF_H_
#define _SRIC_REF_H_

#include <stdint.h>

#define SC_HEAP_MAGIC_CODE 0xF1780126
#define SC_STACK_MAGIC_CODE 0xE672941A
#define SC_INTRUSIVE_MAGIC_CODE 0xB392E928

namespace sric
{

class HeapRefable;

class WeakRefBlock {
    friend class HeapRefable;

    int _weakRefCount;

    HeapRefable* _pointer;
public:
    WeakRefBlock();
    ~WeakRefBlock();

    HeapRefable* lock();

    void addRef();
    void release();
};


class HeapRefable
{
public:

    /**
     * Increments the reference count of this object.
     *
     * The release() method must be called when the caller relinquishes its
     * handle to this object in order to decrement the reference count.
     */
    void addRef();

    /**
     * Decrements the reference count of this object.
     *
     * When an object is initially created, its reference count is set to 1.
     * Calling addRef() will increment the reference and calling release()
     * will decrement the reference count. When an object reaches a
     * reference count of zero, the object is destroyed.
     */
    bool release();

    /**
     * Returns the current reference count of this object.
     *
     * @return This object's reference count.
     */
    unsigned int getRefCount() const;

    void _setRefCount(int rc);

    WeakRefBlock* getWeakRefBlock();

public:

    /**
     * Constructor.
     */
    HeapRefable();

    /**
     * Destructor.
     */
    ~HeapRefable();

private:
    void disposeWeakRef();

private:
    template<typename T> friend class RefPtr;
    template <typename T> friend class DArray;

    uint32_t _checkCode;

    //valid array content byte size
    uint32_t _dataSize;

private:
    //is only one reference
    bool _isUnique;

public:
    uint32_t _magicCode;
private:
    int _refCount;

    WeakRefBlock* _weakRefBlock;

public:
    void (*freeMemory)(void*);
    void (*dealloc)(HeapRefable*);
private:
    // Memory leak diagnostic data
#ifdef SC_USE_REF_TRACE
    friend void* trackRef(HeapRefable* ref);
    friend void untrackRef(HeapRefable* ref);
    HeapRefable* _next;
    HeapRefable* _prev;
public:
    static void printLeaks();
#endif
};

}

#endif
