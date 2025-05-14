#ifndef _SRIC_REF_H_
#define _SRIC_REF_H_

#include <stdint.h>
#include "sric/RefCount.h"

#define SC_HEAP_MAGIC_CODE 0xF1780126
#define SC_STACK_MAGIC_CODE 0xE672941A
#define SC_INTRUSIVE_MAGIC_CODE 0xB392E928

namespace sric
{

uint32_t generateCheckCode();

template<typename T>
class RefPtr;

struct ObjBase {
    uint32_t __magicCode = SC_INTRUSIVE_MAGIC_CODE;
    uint32_t __checkCode = sric::generateCheckCode();

    ~ObjBase() {
        __magicCode = 0;
        __checkCode = 0;
    }
};

template<typename T>
struct StackRefable {
    uint32_t _magicCode;
    uint32_t checkCode;
    T value;

    StackRefable() : checkCode(generateCheckCode()), _magicCode(SC_STACK_MAGIC_CODE) {}

    StackRefable(const T& v) : value(v), checkCode(generateCheckCode()), _magicCode(SC_STACK_MAGIC_CODE) {
    }

    ~StackRefable() {
        checkCode = 0;
        _magicCode = 0;
    }

    StackRefable& operator=(const T& v) {
        value = v;
        return *this;
    }

    StackRefable& operator=(T&& v) {
        value = std::move(v);
        return *this;
    }

    T* operator->() const { return &value; }

    T* operator->() { return &value; }

    T& operator*() { return value; }

    const T& operator*() const { return value; }

    operator T () { return value; }

    RefPtr<T> operator&() { return RefPtr<T>(*this); }

    RefPtr<T> getPtr() { return RefPtr<T>(*this); }
};

class HeapRefable {
public:
    RefCount* _refCount = nullptr;
public:
#ifndef SC_NO_CHECK
    uint32_t _magicCode = SC_HEAP_MAGIC_CODE;
    uint32_t _checkCode = generateCheckCode();
    int32_t _dataSize = 0;
#endif

    RefCount* getRefCount() {
        if (_refCount == nullptr) {
            _refCount = new RefCount();
            _refCount->_pointer = this;
        }
        return _refCount;
    }
    
    ~HeapRefable() {
        _checkCode = 0;
    }
};

}

#endif
