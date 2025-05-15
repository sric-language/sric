#include "sric/RefCount.h"
#include <mutex>
#include "sric/common.h"

#define SC_REFCOUNT_INVALID 1000000

namespace sric
{

std::mutex traceLock;

RefCount::RefCount() :
    _refCount(1), _pointer(NULL), freeMemory(NULL), destructor(NULL)
{
}

RefCount::~RefCount()
{
    _refCount = SC_REFCOUNT_INVALID;
}

void RefCount::addRef()
{
    sc_assert(_refCount > 0 && _refCount < SC_REFCOUNT_INVALID, "ref count error");
    ++_refCount;
}

bool RefCount::release()
{
    sc_assert(_refCount > 0 && _refCount < SC_REFCOUNT_INVALID, "ref count error");
    if (--_refCount == 0)
    {
        //delete this;
        tryDelete();
        return true;
    }
    return false;
}

void RefCount::addWeakRef() {
    ++_weakRefCount;
}

bool RefCount::releaseWeak() {
    if (--_weakRefCount == 0) {
        return tryDelete();
    }
    return false;
}

bool RefCount::lock() {
    if (_refCount == 0) {
        return false;
    }
    std::lock_guard<std::mutex> guard(traceLock);
    ++_refCount;
    if (_refCount > 1) {
        return true;
    }
    _refCount = 0;
    return false;
}

bool RefCount::tryDelete() {
    std::lock_guard<std::mutex> guard(traceLock);
    if (_weakRefCount == 0 && _refCount == 0) {
        delete this;
        return true;
    }
    return false;
}

}
