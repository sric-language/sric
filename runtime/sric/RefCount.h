#ifndef _SRIC_REFCOUNT_H_
#define _SRIC_REFCOUNT_H_

#include <stdint.h>
#include <atomic>

namespace sric
{

class RefCount
{
private:
    template<typename T> friend class RefPtr;
    template <typename T> friend class DArray;

    std::atomic_int32_t _weakRefCount;

    std::atomic_int32_t _refCount;

public:
    void* _pointer;
    void (*freeMemory)(void*);
    void (*destructor)(void*);
public:
    RefCount();
    ~RefCount();

    void addRef();
    bool release();

    void addWeakRef();
    bool releaseWeak();
    bool lock();

    unsigned int getRefCount() const { return _refCount; }
    void _setRefCount(int rc) { _refCount = rc; }
private:
    bool tryDelete();


};

}

#endif
