#ifndef _SRIC_REFCOUNT_H_
#define _SRIC_REFCOUNT_H_

#include <stdint.h>
#include <atomic>
#include "sric/common.h"

#define SC_REFCOUNT_INVALID 1000000

namespace sric
{
class HeapRefable;

class RefCount
{
private:
    template<typename T> friend class RefPtr;
    template <typename T> friend class DArray;
    friend class HeapRefable;
    template<typename T> friend class OwnPtr;
    template<typename T> friend class WeakPtr;

    std::atomic_int32_t _weakRefCount;

    std::atomic_int32_t _refCount;

    HeapRefable* _pointer;
    //void (*freeMemory)(void*);
    
public:
    inline RefCount() : _refCount(1), _pointer(NULL) {}
    inline ~RefCount() {
        _refCount = SC_REFCOUNT_INVALID;
    }

    inline void addRef() {
        sc_assert(_refCount > 0 && _refCount < SC_REFCOUNT_INVALID, "ref count error");
        ++_refCount;
    }
    inline bool release() {
        sc_assert(_refCount > 0 && _refCount < SC_REFCOUNT_INVALID, "ref count error");
        if (--_refCount == 0)
        {
            //delete this;
            tryDelete();
            return true;
        }
        return false;
    }

    inline void addWeakRef() {
        ++_weakRefCount;
    }
    inline bool releaseWeak() {
        if (--_weakRefCount == 0) {
            return tryDelete();
        }
        return false;
    }
    bool lock();

    unsigned int getRefCount() const { return _refCount; }
    void _setRefCount(int rc) { _refCount = rc; }
private:
    bool tryDelete();


};

}

#endif
