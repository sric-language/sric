#include "sric/RefCount.h"
#include <mutex>


namespace sric
{

std::mutex traceLock;

bool RefCount::lock() {
    sc_assert(_weakRefCount > 0, "weak ref count error");

    if (_refCount == 0) {
        return false;
    }
    std::lock_guard<std::mutex> guard(traceLock);
    ++_refCount;
    if (_refCount == 1) {
        _refCount = 0;
        return false;
    }
    return true;
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
