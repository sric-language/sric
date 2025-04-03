/*
 * Copyright (c) 2012-2016, chunquedong
 *
 * This file is part of cppfan project
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE version 3.0
 *
 * History:
 *   2012-12-23  Jed Young  Creation
 */
#ifndef _SRIC_REFPTR_H_
#define _SRIC_REFPTR_H_

#include "Ptr.h"

#define SC_SELF_TYPE std::remove_reference<decltype(*this)>::type
#define SC_SAFE_STRUCT

#ifdef SC_NO_CHECK
    #define SC_OBJ_BASE
    #define SC_BEGIN_INHERIT :
    #define SC_BEGIN_METHOD() auto __this = sric::makeRefPtr(this)
#else
    #define SC_OBJ_BASE : public sric::ObjBase
    #define SC_BEGIN_INHERIT ,
    #define SC_BEGIN_METHOD() auto __this = sric::makeRefPtr(this)
    
#endif // SC_NO_CHECK

#ifdef SC_CHECK
    #define SC_THIS __this->
#else
    #define SC_THIS
#endif

namespace sric
{
template <typename T, typename = void>
struct has_checkcode : std::false_type {};

template <typename T>
struct has_checkcode<T, std::void_t<decltype(std::declval<T>().__checkCode)>>
    : std::true_type {};

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

    StackRefable(): checkCode(generateCheckCode()), _magicCode(SC_STACK_MAGIC_CODE) {}

    StackRefable(const T& v): value(v), checkCode(generateCheckCode()), _magicCode(SC_STACK_MAGIC_CODE) {
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

/////////////////////////////////////////////////////////////////////////////////////////////////////

enum struct RefType
{
    HeapRef, ArrayRef, StackRef, UnsafeRef, IntrusiveRef
};

template<typename T>
class RefPtr {
    T* pointer;
#ifndef SC_NO_CHECK
    uint32_t checkCode;
    uint32_t offset;
    RefType type;
#endif // !SC_NO_CHECK

    template <class U> friend class RefPtr;
    template <class U> friend RefPtr<U> rawToRef(U* ptr);
    template <class U> friend OwnPtr<U> refToOwn(RefPtr<U> ptr);
private:
#ifdef SC_CHECK
    void onDeref() const {
        sc_assert(pointer != nullptr, "try access null pointer 0");
        switch (type) {
        case RefType::HeapRef : {
            T* first = (T*)(((char*)pointer) - offset);
            HeapRefable* refable = getRefable(first);
            sc_assert(checkCode == refable->_checkCode, "try access invalid pointer 1");
            break;
        }
        case RefType::StackRef: {
            T* first = (T*)(((char*)pointer) - offset);
            sc_assert(checkCode == *(((int32_t*)first) - 1), "try access invalid pointer 2");
            break;
        }
        case RefType::ArrayRef: {
            T* first = (T*)(((char*)pointer) - offset);
            HeapRefable* refable = getRefable(first);
            sc_assert(checkCode == refable->_checkCode, "try access invalid array element pointer 1");
            sc_assert(offset < refable->_dataSize, "try access invalid array element pointer 2");
            break;
        }
        case RefType::IntrusiveRef: {
            if constexpr (has_checkcode<T>::value) {
                if (offset == 0) {
                    sc_assert(checkCode == pointer->__checkCode, "try access invalid pointer 3");
                }
                else {
                    T* first = (T*)(((char*)pointer) - offset);
                    int32_t* code = (int32_t*)toVoid(first);
                    sc_assert(checkCode == *(code + 1), "try access invalid pointer 4");
                }
            }
            else if constexpr (std::is_polymorphic<T>::value) {
                T* first = (T*)(((char*)pointer) - offset);
                ObjBase* code = (ObjBase*)dynamic_cast<ObjBase*>(first);
                sc_assert(checkCode == code->__checkCode, "try access invalid pointer 5");
            }
            else {
                T* first = (T*)(((char*)pointer) - offset);
                int32_t* code = (int32_t*)toVoid(first);
                sc_assert(checkCode == *(code + 1), "try access invalid pointer 5");
            }
            break;
        }
        }
    }
#endif

public:

    RefPtr() : pointer(nullptr)
#ifndef SC_NO_CHECK
        , checkCode(0), offset(0), type(RefType::UnsafeRef) 
#endif
    {
    }

    RefPtr(T* p, uint32_t checkCode, RefType type, uint32_t offset = 0) : pointer(p)
#ifndef SC_NO_CHECK
        , checkCode(checkCode), offset(offset), type(type) 
#endif
    {
    }

    RefPtr(T* p, uint32_t checkCode, uint32_t arrayOffset) : pointer(p)
#ifndef SC_NO_CHECK
        , checkCode(checkCode), offset(arrayOffset), type(RefType::ArrayRef)
#endif
    {
    }

    RefPtr(StackRefable<T>& p) : pointer(&p.value)
#ifndef SC_NO_CHECK
        , checkCode(p.checkCode), offset(0), type(RefType::StackRef)
#endif
    {
    }

    RefPtr(HeapRefable *r) : pointer((T*)(r+1))
#ifndef SC_NO_CHECK
        , checkCode(r->_checkCode), offset(0), type(RefType::HeapRef) 
#endif
    {
    }

    template <class U>
    RefPtr(const OwnPtr<U>& p)
#ifndef SC_NO_CHECK
        : offset(0) 
#endif
    {
        if (p.isNull()) {
            pointer = nullptr;
#ifndef SC_NO_CHECK
            type = RefType::UnsafeRef;
            checkCode = 0;
#endif
        }
        else {
            pointer = p.get();
#ifndef SC_NO_CHECK
            type = RefType::HeapRef;
            checkCode = getRefable(pointer)->_checkCode;
#endif
        }
    }

    template <class U>
    RefPtr(const OwnPtr<U>& p, T* ptr) : pointer(ptr) {
#ifndef SC_NO_CHECK
        sc_assert(p.get(), "try access null pointer");
        type = (RefType::HeapRef);
        offset = (char*)ptr - (char*)p.get();
        checkCode = getRefable(p.get())->_checkCode;
#endif
    }

    template <class U>
    RefPtr(const RefPtr<U>& p) : pointer(p.pointer)
#ifndef SC_NO_CHECK
        , checkCode(p.checkCode), offset(p.offset), type(p.type)
#endif
    {
    }

    template <class U>
    RefPtr(const RefPtr<U>& p, T* ptr) : pointer(ptr)
#ifndef SC_NO_CHECK
        , checkCode(p.checkCode), type(p.type)
#endif
    {
#ifndef SC_NO_CHECK
        offset = p.offset + (char*)ptr - (char*)p.get();
#endif
    }

    T* operator->() const {
#ifdef SC_CHECK
        onDeref();
#endif
        return pointer;
    }

    T* operator->() {
#ifdef SC_CHECK
        onDeref();
#endif
        return pointer;
    }

    T& operator*() { 
#ifdef SC_CHECK
        onDeref();
#endif
        return *pointer;
    }

    operator T* () { return pointer; }

    T* get() const {
#ifdef SC_CHECK
        if (pointer)
            onDeref();
#endif
        return pointer;
    }

    bool isNull() const { return pointer == nullptr; }

    bool operator==(const T* other) { return this->pointer == other; }
    bool operator==(const RefPtr<T>& other) { return this->pointer == other.pointer; }
    bool operator<(const RefPtr<T>& other) { return this->pointer < other.pointer; }

    template <class U> RefPtr<U> castTo()
    {
        if constexpr (std::is_polymorphic<U>::value) {
#ifndef SC_NO_CHECK
            return RefPtr<U>(dynamic_cast<U*>(pointer), checkCode, type, offset);
#else
            return RefPtr<U>(dynamic_cast<U*>(pointer), 0, RefType::UnsafeRef, 0);
#endif
        }
        else {
#ifndef SC_NO_CHECK
            return RefPtr<U>((U*)(pointer), checkCode, type, offset);
#else
            return RefPtr<U>((U*)(pointer), 0, RefType::UnsafeRef, 0);
#endif
        }
    }

    template <class U> RefPtr<U> dynamicCastTo()
    {
#ifndef SC_NO_CHECK
        return RefPtr<U>(dynamic_cast<U*>(pointer), checkCode, type, offset);
#else
        return RefPtr<U>(dynamic_cast<U*>(pointer), 0, RefType::UnsafeRef, 0);
#endif
    }
};

template<>
class RefPtr<void> {
    void* pointer;
#ifndef SC_NO_CHECK
    uint32_t checkCode;
    uint32_t offset;
    RefType type;
#endif
    template <class U> friend class RefPtr;
    template <class U> friend RefPtr<U> rawToRef(U* ptr);
    template <class U> friend OwnPtr<U> refToOwn(RefPtr<U> ptr);
private:
#ifdef SC_CHECK
    void onDeref() const {
        sc_assert(pointer != nullptr, "try access null pointer");
        switch (type) {
        case RefType::HeapRef : {
            void* first = (void*)(((char*)pointer) - offset);
            HeapRefable* refable = getRefable(first);
            sc_assert(checkCode == refable->_checkCode, "try access invalid pointer");
            break;
        }
        case RefType::StackRef: {
            void* first = (void*)(((char*)pointer) - offset);
            sc_assert(checkCode == *(((int32_t*)first) - 1), "try access invalid pointer");
            break;
        }
        case RefType::ArrayRef: {
            void* first = (void*)(((char*)pointer) - offset);
            HeapRefable* refable = getRefable(first);
            sc_assert(checkCode == refable->_checkCode, "try access invalid array element pointer");
            sc_assert(offset < refable->_dataSize, "try access invalid array element pointer");
            break;
        }
        case RefType::IntrusiveRef: {
            /*void* first = (void*)(((char*)pointer) - offset);
            int32_t* code = (int32_t*)first;
            sc_assert(checkCode == *(code + 1), "try access invalid pointer");*/
            break;
        }
        }
    }
#endif

public:

    RefPtr() : pointer(nullptr)
#ifndef SC_NO_CHECK
        , checkCode(0), offset(0), type(RefType::UnsafeRef) 
#endif
    {
    }

    RefPtr(void* p, uint32_t checkCode, RefType type, uint32_t offset = 0) : pointer(p)
#ifndef SC_NO_CHECK
        , checkCode(checkCode), offset(offset), type(type) 
#endif
    {
    }

    RefPtr(void* p, uint32_t checkCode, uint32_t arrayOffset) : pointer(p)
#ifndef SC_NO_CHECK
        , checkCode(checkCode), offset(arrayOffset), type(RefType::ArrayRef)
#endif
    {
    }

    RefPtr(HeapRefable *r) : pointer((void*)(r+1))
#ifndef SC_NO_CHECK
        , checkCode(r->_checkCode), offset(0), type(RefType::HeapRef)
#endif
    {
    }

    template <class U>
    RefPtr(const OwnPtr<U>& p)
#ifndef SC_NO_CHECK
        : offset(0)
#endif
    {
        if (p.isNull()) {
            pointer = nullptr;
#ifndef SC_NO_CHECK
            type = RefType::UnsafeRef;
            checkCode = 0;
#endif
        }
        else {
            pointer = p.get();
#ifndef SC_NO_CHECK
            type = RefType::HeapRef;
            checkCode = getRefable(pointer)->_checkCode;
#endif
        }
    }

    template <class U>
    RefPtr(const OwnPtr<U>& p, void* ptr) : pointer(ptr) {
#ifndef SC_NO_CHECK
        sc_assert(p.get(), "try access null pointer");
        type = (RefType::HeapRef);
        offset = (char*)ptr - (char*)p.get();
        checkCode = getRefable(p.get())->_checkCode;
#endif
    }

    template <class U>
    RefPtr(const RefPtr<U>& p) : pointer(p.pointer)
#ifndef SC_NO_CHECK
        , checkCode(p.checkCode), offset(p.offset), type(p.type) 
#endif
    {
    }

    template <class U>
    RefPtr(const RefPtr<U>& p, void* ptr) : pointer(ptr)
#ifndef SC_NO_CHECK
        , checkCode(p.checkCode), type(p.type) 
#endif
    {
#ifndef SC_NO_CHECK
        offset = p.offset + (char*)ptr - (char*)p.get();
#endif
    }

    void* operator->() const {
#ifdef SC_CHECK
        onDeref();
#endif
        return pointer;
    }

    void* operator->() {
#ifdef SC_CHECK
        onDeref();
#endif
        return pointer;
    }

    operator void* () {
        return pointer;
    }

    void* get() const {
#ifdef SC_CHECK
        if (pointer)
            onDeref();
#endif
        return pointer;
    }

    bool isNull() const { return pointer == nullptr; }

    bool operator==(const void* other) { return this->pointer == other; }
    bool operator==(const RefPtr<void>& other) { return this->pointer == other.pointer; }
    bool operator<(const RefPtr<void>& other) { return this->pointer < other.pointer; }

    template <class U> RefPtr<U> castTo()
    {
        if constexpr (std::is_polymorphic<U>::value) {
#ifndef SC_NO_CHECK
            return RefPtr<U>(dynamic_cast<U*>(pointer), checkCode, type, offset);
#else
            return RefPtr<U>(dynamic_cast<U*>(pointer), 0, RefType::UnsafeRef, 0);
#endif
        }
        else {
#ifndef SC_NO_CHECK
            return RefPtr<U>((U*)(pointer), checkCode, type, offset);
#else
            return RefPtr<U>((U*)(pointer), 0, RefType::UnsafeRef, 0);
#endif
        }
    }

    template <class U> RefPtr<U> dynamicCastTo()
    {
#ifndef SC_NO_CHECK
        return RefPtr<U>(dynamic_cast<U*>(pointer), checkCode, type, offset);
#else
        return RefPtr<U>(dynamic_cast<U*>(pointer), 0, RefType::UnsafeRef, 0);
#endif
    }
};

template <class T>
OwnPtr<T> refToOwn(RefPtr<T> ptr) {
    HeapRefable* r = getRefable(ptr.get());
#ifndef SC_NO_CHECK
    if (r->_magicCode != SC_HEAP_MAGIC_CODE) {
        fprintf(stderr, "ERROR: Can't cast ref pointer to own pointer\n");
        abort();
        return OwnPtr<T>();
    }
#endif
    r->addRef();
    return OwnPtr<T>(ptr.get());
}

template<typename T>
RefPtr<T> addressOf(T& b) {
#ifndef SC_NO_CHECK
    return sric::RefPtr<T>(&b, b.__checkCode, sric::RefType::IntrusiveRef);
#else
    return sric::RefPtr<T>(&b, 0, sric::RefType::UnsafeRef);
#endif
}

template<typename T>
RefPtr<T> makeRefPtr(T* b) {
#ifndef SC_NO_CHECK
    return sric::RefPtr<T>(b, b->__checkCode, sric::RefType::IntrusiveRef);
#else
    return sric::RefPtr<T>(b, 0, sric::RefType::UnsafeRef);
#endif
}

template <class T>
RefPtr<T> rawToRef(T* ptr) {
    if constexpr (has_checkcode<T>::value) {
        return addressOf<T>(*ptr);
    }

    HeapRefable *r = getRefable(ptr);
    if (r->_magicCode == SC_HEAP_MAGIC_CODE) {
        return RefPtr<T>(r);
    }

    void *p = toVoid(ptr);
    uint32_t *code = ((uint32_t*)p) - 2;
    if (*code == SC_STACK_MAGIC_CODE) {
        return RefPtr<T>(ptr, *(code-1), RefType::StackRef);
    }

    sc_assert(false, "Can't cast raw pointer to ref pointer");
    return RefPtr<T>(ptr, 0, RefType::UnsafeRef);
}

}
#endif