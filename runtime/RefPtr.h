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

namespace sric
{

uint32_t generateCheckCode();

template<typename T>
class RefPtr;

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

    operator T () { return value; }

    RefPtr<T> operator&() { return RefPtr<T>(*this); }

    RefPtr<T> getRef() { return RefPtr<T>(*this); }
};

/////////////////////////////////////////////////////////////////////////////////////////////////////

enum struct RefType
{
    HeapRef, ArrayRef, StackRef, RawRef
};

template<typename T>
class RefPtr {
    T* pointer;
    uint32_t checkCode;
    uint32_t offset;
    RefType type;

    template <class U> friend class RefPtr;
    template <class U> friend RefPtr<U> rawToRef(U* ptr);
    template <class U> friend OwnPtr<U> refToOwn(RefPtr<U> ptr);
private:
#ifdef SC_NO_CHECK
#else
    void onDeref() const {
        sc_assert(pointer != nullptr, "try access null pointer");
        switch (type) {
        case RefType::HeapRef : {
            T* first = (T*)(((char*)pointer) - offset);
            HeapRefable* refable = getRefable(first);
            sc_assert(checkCode == refable->_checkCode, "try access invalid pointer");
            break;
        }
        case RefType::StackRef: {
            T* first = (T*)(((char*)pointer) - offset);
            sc_assert(checkCode == *(((int32_t*)first) - 1), "try access invalid pointer");
            break;
        }
        case RefType::ArrayRef: {
            T* first = (T*)(((char*)pointer) - offset);
            HeapRefable* refable = getRefable(first);
            sc_assert(checkCode == refable->_checkCode, "try access invalid array element pointer");
            sc_assert(offset < refable->_dataSize, "try access invalid array element pointer");
            break;
        }
        }
    }
#endif
private:
    RefPtr(T* p) : pointer(p), checkCode(0), offset(0), type(RefType::RawRef) {
    }
public:

    RefPtr() : pointer(nullptr), checkCode(0), offset(0), type(RefType::RawRef) {
    }

    RefPtr(T* p, uint32_t checkCode, uint32_t arrayOffset) : pointer(p), checkCode(checkCode), offset(arrayOffset), type(RefType::ArrayRef) {
    }

    RefPtr(StackRefable<T>& p) : pointer(&p.value), checkCode(p.checkCode), offset(0), type(RefType::StackRef) {
    }

    RefPtr(HeapRefable *r) : pointer((T*)(r+1)), checkCode(r->_checkCode), offset(0), type(RefType::HeapRef) {
    }

    template <class U>
    RefPtr(const OwnPtr<U>& p) : offset(0) {
        if (p.isNull()) {
            pointer = nullptr;
            type = RefType::RawRef;
            checkCode = 0;
        }
        else {
            pointer = p.get();
            type = RefType::HeapRef;
            checkCode = getRefable(pointer)->_checkCode;
        }
    }

    template <class U>
    RefPtr(const OwnPtr<U>& p, T* ptr) : pointer(ptr), type(RefType::HeapRef) {
        sc_assert(p.get(), "try access null pointer");
        offset = (char*)ptr - (char*)p.get();
        checkCode = getRefable(p.get())->_checkCode;
    }

    template <class U>
    RefPtr(const RefPtr<U>& p) : pointer(p.pointer), checkCode(p.checkCode), offset(p.offset), type(p.type) {
    }

    template <class U>
    RefPtr(const RefPtr<U>& p, T* ptr) : checkCode(p.checkCode), pointer(ptr), type(p.type) {
        offset = p.offset + (char*)ptr - (char*)p.get();
    }

    T* operator->() const {
#ifndef SC_NO_CHECK
        onDeref();
#endif
        return pointer;
    }

    T* operator->() {
#ifndef SC_NO_CHECK
        onDeref();
#endif
        return pointer;
    }

    T& operator*() { 
#ifndef SC_NO_CHECK
        onDeref();
#endif
        return *pointer;
    }

    operator T* () { return pointer; }

    T* get() const { return pointer; }

    bool isNull() const { return pointer == nullptr; }

    bool operator==(const T* other) { return this->pointer == other; }
    bool operator==(const RefPtr<T>& other) { return this->pointer == other.pointer; }
    bool operator<(const RefPtr<T>& other) { return this->pointer < other.pointer; }

    template <class U> RefPtr<U> castTo()
    {
        RefPtr<U> copy((U*)(pointer));
        copy.checkCode = checkCode;
        copy.type = type;
        return copy;
    }

    template <class U> RefPtr<U> dynamicCastTo()
    {
        RefPtr<U> copy(dynamic_cast<U*>(pointer));
        copy.checkCode = checkCode;
        copy.type = type;
        return copy;
    }
};

template<>
class RefPtr<void> {
    void* pointer;
    uint32_t checkCode;
    uint32_t offset;
    RefType type;

    template <class U> friend class RefPtr;
    template <class U> friend RefPtr<U> rawToRef(U* ptr);
    template <class U> friend OwnPtr<U> refToOwn(RefPtr<U> ptr);
private:
#ifdef SC_NO_CHECK
#else
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
        }
    }
#endif
private:
    RefPtr(void* p) : pointer(p), checkCode(0), offset(0), type(RefType::RawRef) {
    }
public:

    RefPtr() : pointer(nullptr), checkCode(0), offset(0), type(RefType::RawRef) {
    }

    RefPtr(void* p, uint32_t checkCode, uint32_t arrayOffset) : pointer(p), checkCode(checkCode), offset(arrayOffset), type(RefType::ArrayRef) {
    }

    RefPtr(HeapRefable *r) : pointer((void*)(r+1)), checkCode(r->_checkCode), offset(0), type(RefType::HeapRef) {
    }

    template <class U>
    RefPtr(const OwnPtr<U>& p) : offset(0) {
        if (p.isNull()) {
            pointer = nullptr;
            type = RefType::RawRef;
            checkCode = 0;
        }
        else {
            pointer = p.get();
            type = RefType::HeapRef;
            checkCode = getRefable(pointer)->_checkCode;
        }
    }

    template <class U>
    RefPtr(const OwnPtr<U>& p, void* ptr) : pointer(ptr), type(RefType::HeapRef) {
        sc_assert(p.get(), "try access null pointer");
        offset = (char*)ptr - (char*)p.get();
        checkCode = getRefable(p.get())->_checkCode;
    }

    template <class U>
    RefPtr(const RefPtr<U>& p) : pointer(p.pointer), checkCode(p.checkCode), offset(p.offset), type(p.type) {
    }

    template <class U>
    RefPtr(const RefPtr<U>& p, void* ptr) : checkCode(p.checkCode), pointer(ptr), type(p.type) {
        offset = p.offset + (char*)ptr - (char*)p.get();
    }

    void* operator->() const {
#ifndef SC_NO_CHECK
        onDeref();
#endif
        return pointer;
    }

    void* operator->() {
#ifndef SC_NO_CHECK
        onDeref();
#endif
        return pointer;
    }

    operator void* () { return pointer; }

    void* get() const { return pointer; }

    bool isNull() const { return pointer == nullptr; }

    bool operator==(const void* other) { return this->pointer == other; }
    bool operator==(const RefPtr<void>& other) { return this->pointer == other.pointer; }
    bool operator<(const RefPtr<void>& other) { return this->pointer < other.pointer; }

    template <class U> RefPtr<U> castTo()
    {
        RefPtr<U> copy((U*)(pointer));
        copy.checkCode = checkCode;
        copy.type = type;
        return copy;
    }

    template <class U> RefPtr<U> dynamicCastTo()
    {
        RefPtr<U> copy(dynamic_cast<U*>(pointer));
        copy.checkCode = checkCode;
        copy.type = type;
        return copy;
    }
};

template <class T>
OwnPtr<T> refToOwn(RefPtr<T> ptr) {
    if (ptr.type != RefType::HeapRef) {
        sc_assert(false, "Can't cast ref pointer to own pointer");
        return OwnPtr<T>();
    }
    getRefable(ptr.get())->addRef();
    return OwnPtr<T>(ptr.get());
}

template <class T>
RefPtr<T> rawToRef(T* ptr) {

    //StackRefable<T>* sr = (StackRefable<T>*)ptr;
    //if (sr->_magicCode == SC_STACK_MAGIC_CODE) {
    //    return RefPtr<T>(*sr);
    //}

    HeapRefable *r = getRefable(ptr);
    if (r->_magicCode == SC_HEAP_MAGIC_CODE) {
        return RefPtr<T>(r);
    }

    sc_assert(false, "Can't cast raw pointer to ref pointer");
    return RefPtr<T>(ptr);
}

}
#endif