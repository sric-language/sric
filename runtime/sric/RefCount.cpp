#include "sric/RefCount.h"
#include <mutex>


namespace sric
{

std::mutex traceLock;

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
