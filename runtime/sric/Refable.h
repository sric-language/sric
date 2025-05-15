#ifndef _SRIC_REF_H_
#define _SRIC_REF_H_

#include <stdint.h>
#include "sric/RefCount.h"

#define SC_HEAP_MAGIC_CODE 0xF1780126
#define SC_STACK_MAGIC_CODE 0xE672941A
#define SC_INTRUSIVE_MAGIC_CODE 0xB392E928

//#define SC_SELF_TYPE std::remove_reference<decltype(*this)>::type
#define SC_SAFE_STRUCT
#define SC_DEFINE_THIS_REFPTR auto __self = sric::makeRefPtr(this);

#ifdef SC_NO_CHECK
#define SC_OBJ_BASE
#define SC_BEGIN_INHERIT :
#else
#define SC_OBJ_BASE : public sric::ObjBase
#define SC_BEGIN_INHERIT ,
#endif // SC_NO_CHECK

#ifdef SC_CHECK
#define SC_DEFINE_THIS auto __self = sric::makeRefPtr(this);
#define sc_this __self
#define sc_thisref __self
#else
#define SC_DEFINE_THIS
#define sc_this this
#define sc_thisref __self
#endif

namespace sric
{
template <typename T, typename = void>
struct has_checkcode : std::false_type {};

template <typename T>
struct has_checkcode<T, std::void_t<decltype(std::declval<T>().__checkCode)>>
    : std::true_type {};


template<typename U>
inline typename std::enable_if<std::is_polymorphic<U>::value, void*>::type toVoid(U* pointer) {
    return dynamic_cast<void*>(pointer);
}

template<typename U>
inline typename std::enable_if<!std::is_polymorphic<U>::value, void*>::type toVoid(U* pointer) {
    return (void*)pointer;
}


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
