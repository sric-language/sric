#include "Refable.h"
#include <mutex>
#include "common.h"
#include <thread>

namespace sric
{
std::mutex traceLock;
#ifdef GP_USE_REF_TRACE
void* trackRef(HeapRefable* ref);
void untrackRef(HeapRefable* ref);
#endif

uint32_t checkCodeCount = 0;

uint32_t generateCheckCode() {
    std::hash<std::thread::id> hasher;
    size_t tid = hasher(std::this_thread::get_id());
    return (tid << 24) | (++checkCodeCount);
}

HeapRefable::HeapRefable() :
    _refCount(1), _isUnique(true), _weakRefBlock(NULL)
{
    _checkCode = generateCheckCode();
#ifdef GP_USE_REF_TRACE
    trackRef(this);
#endif
}

//HeapRefable::HeapRefable(const HeapRefable& copy) :
//    _refCount(1)
//{
//#ifdef GP_USE_REF_TRACE
//    trackRef(this);
//#endif
//}

HeapRefable::~HeapRefable()
{
    if (!_isUnique) {
        sc_assert(_refCount == 0, "ref count error");
    }
    _checkCode = 0;
    _refCount = 1000000;
#ifdef GP_USE_REF_TRACE
    untrackRef(this);
#endif
}

void HeapRefable::addRef()
{
    _isUnique = false;
    sc_assert(_refCount > 0 && _refCount < 1000000, "ref count error");
    ++_refCount;
}

void HeapRefable::disposeWeakRef() {
    if (_weakRefBlock) {
        std::lock_guard<std::mutex> guard(traceLock);
        if (_weakRefBlock->_weakRefCount == 0) {
            delete _weakRefBlock;
        }
        else {
            _weakRefBlock->_pointer = NULL;
        }
    }
}

bool HeapRefable::release()
{
    if (_isUnique) {
        disposeWeakRef();
        //delete this;
        this->~HeapRefable();
        return true;
    }

    sc_assert(_refCount > 0 && _refCount < 1000000, "ref count error");
    if ((--_refCount) <= 0)
    {
        disposeWeakRef();
        //delete this;
        this->~HeapRefable();
        return true;
    }
    return false;
}

void HeapRefable::_setRefCount(int rc) {
    _refCount = rc;
}

unsigned int HeapRefable::getRefCount() const
{
    return _refCount;
}

////////////////////////////////////////////////////////////////////////////////////////

WeakRefBlock* HeapRefable::getWeakRefBlock() {
    if (_weakRefBlock) return _weakRefBlock;
    std::lock_guard<std::mutex> guard(traceLock);
    if (!_weakRefBlock) {
        _weakRefBlock = new WeakRefBlock();
        _weakRefBlock->_pointer = this;
    }
    return _weakRefBlock;
}

WeakRefBlock::WeakRefBlock() : _weakRefCount(0), _pointer(NULL) {
    
}
WeakRefBlock::~WeakRefBlock() {
    sc_assert(_weakRefCount == 0, "ref count error");
    _weakRefCount = 1000000;
    _pointer = NULL;
}

void WeakRefBlock::addRef() {
    sc_assert(_weakRefCount < 1000000, "ref count error");
    ++_weakRefCount;
}

void WeakRefBlock::release() {
    sc_assert(_weakRefCount > 0 && _weakRefCount < 1000000, "ref count error");
    if ((--_weakRefCount) <= 0)
    {
        std::lock_guard<std::mutex> guard(traceLock);
        if (!_pointer) {
            delete this;
        }
    }
}

HeapRefable* WeakRefBlock::lock() {
    std::lock_guard<std::mutex> guard(traceLock);
    if (_pointer) {
        _pointer->addRef();
        return _pointer;
    }
    return NULL;
}

////////////////////////////////////////////////////////////////////////////////////////

#ifdef GP_USE_REF_TRACE

HeapRefable* __refAllocations = 0;
int __refAllocationCount = 0;

void HeapRefable::printLeaks()
{
    std::lock_guard<std::mutex> guard(traceLock);
    // Dump HeapRefable object memory leaks
    if (__refAllocationCount == 0)
    {
        print("[memory] All HeapRefable objects successfully cleaned up (no leaks detected).\n");
    }
    else
    {
        print("[memory] WARNING: %d HeapRefable objects still active in memory.\n", __refAllocationCount);
        for (HeapRefable* rec = __refAllocations; rec != NULL; rec = rec->_next)
        {
            HeapRefable* ref = rec;
            GP_ASSERT(ref);
            const char* type = typeid(*ref).name();
            print("[memory] LEAK: HeapRefable object '%s' still active with reference count %d.\n", (type ? type : ""), ref->getRefCount());
        }
    }
}

void* trackRef(HeapRefable* ref)
{
    std::lock_guard<std::mutex> guard(traceLock);
    GP_ASSERT(ref);

    // Create memory allocation record.
    HeapRefable* rec = ref;
    rec->_next = __refAllocations;
    rec->_prev = NULL;

    if (__refAllocations)
        __refAllocations->_prev = rec;
    __refAllocations = rec;
    ++__refAllocationCount;

    return rec;
}

void untrackRef(HeapRefable* ref)
{
    std::lock_guard<std::mutex> guard(traceLock);
    HeapRefable* rec = ref;

    // Link this item out.
    if (__refAllocations == rec)
        __refAllocations = rec->_next;
    if (rec->_prev)
        rec->_prev->_next = rec->_next;
    if (rec->_next)
        rec->_next->_prev = rec->_prev;

    --__refAllocationCount;
}

#endif

}
