#include "sric/RefCount.h"
#include <mutex>
#include "sric/common.h"

#define SC_REFCOUNT_INVALID 1000000

namespace sric
{

std::mutex traceLock;

RefCount::RefCount() :
    _refCount(1), _isUnique(true), _pointer(NULL), freeMemory(0)
{
}

RefCount::~RefCount()
{
    if (!_isUnique) {
        sc_assert(_refCount == 0, "ref count error");
    }
    _refCount = SC_REFCOUNT_INVALID;
}

void RefCount::addRef()
{
    _isUnique = false;
    sc_assert(_refCount > 0 && _refCount < SC_REFCOUNT_INVALID, "ref count error");
    ++_refCount;
}

bool RefCount::release()
{
    if (_isUnique) {
        //delete this;
        tryDelete();
        return true;
    }

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
