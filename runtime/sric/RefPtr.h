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

#include "sric/Ptr.h"
#include "sric/Refable.h"

namespace sric
{

    enum struct RefType
    {
        HeapRef, ArrayRef, StackRef, UnsafeRef, IntrusiveRef
    };

    template<typename T>
    class RefPtr {
        T* pointer;
#ifndef SC_NO_CHECK
        uint32_t checkCode;
        int32_t offset;
        RefType type;
#endif // !SC_NO_CHECK

        template <class U> friend class RefPtr;
        template <class U> friend RefPtr<U> rawToRef(U* ptr);
        template <class U> friend OwnPtr<U> refToOwn(RefPtr<U> ptr);
    private:
#ifdef SC_CHECK
        inline void onDeref() const {
            sc_assert(pointer != nullptr, "try access null pointer 0");
            uint32_t* code = (uint32_t*)((char*)pointer + offset);
            sc_assert(checkCode == *code, "try access invalid pointer 1");

            if (type == RefType::ArrayRef) {
                int codeOffset = (int)(&((HeapRefable*)nullptr)->_checkCode);
                HeapRefable* refable = (HeapRefable*)((char*)code - codeOffset);
                int dataOffset = (char*)pointer - (char*)(refable + 1);
                sc_assert(dataOffset < refable->_dataSize, "try access invalid array element pointer 2");
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

        RefPtr(T* p, const uint32_t* checkCodePtr, RefType type) : pointer(p)
#ifndef SC_NO_CHECK
            , checkCode(*checkCodePtr), offset((char*)checkCodePtr - (char*)pointer), type(type)
#endif
        {
        }

        RefPtr(T* p, uint32_t checkCode, RefType type, int offset) : pointer(p)
#ifndef SC_NO_CHECK
            , checkCode(checkCode), offset(offset), type(type)
#endif
        {
        }

        RefPtr(StackRefable<T>& p) : pointer(&p.value)
#ifndef SC_NO_CHECK
            , checkCode(p.checkCode), offset((char*)&(p.checkCode) - (char*)pointer), type(RefType::StackRef)
#endif
        {
        }

        RefPtr(HeapRefable* r) : pointer((T*)(r + 1))
#ifndef SC_NO_CHECK
            , checkCode(r->_checkCode), offset((char*)&(r->_checkCode) - (char*)pointer), type(RefType::HeapRef)
#endif
        {
        }

        template <class U>
        RefPtr(const OwnPtr<U>& p) : pointer(p.get())
        {
#ifndef SC_NO_CHECK
            if (p.isNull()) {
                type = RefType::UnsafeRef;
                checkCode = 0;
                offset = 0;
            }
            else {
                type = RefType::HeapRef;
                uint32_t* checkCopePtr = &(sc_getRefable(pointer)->_checkCode);
                checkCode = *checkCopePtr;
                offset = ((char*)checkCopePtr - (char*)pointer);
            }
#endif
        }

        template <class U>
        RefPtr(const OwnPtr<U>& p, T* ptr) : pointer(ptr) {
#ifndef SC_NO_CHECK
            sc_assert(p.get(), "try access null pointer");
            type = (RefType::HeapRef);
            uint32_t* checkCopePtr = &(sc_getRefable(p.get())->_checkCode);
            checkCode = *checkCopePtr;
            offset = ((char*)checkCopePtr - (char*)pointer);
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
            offset = p.offset + (char*)p.get() - (char*)pointer;
#endif
        }

        inline T* operator->() const {
#ifdef SC_CHECK
            onDeref();
#endif
            return pointer;
        }

        inline T* operator->() {
#ifdef SC_CHECK
            onDeref();
#endif
            return pointer;
        }

        inline T& operator*() {
#ifdef SC_CHECK
            onDeref();
#endif
            return *pointer;
        }

        inline operator T* () { return pointer; }

        inline T* get() const {
#ifdef SC_CHECK
            if (pointer)
                onDeref();
#endif
            return pointer;
        }

        inline bool isNull() const { return pointer == nullptr; }

        bool operator==(const T* other) { return this->pointer == other; }
        bool operator==(const RefPtr<T>& other) { return this->pointer == other.pointer; }
        bool operator<(const RefPtr<T>& other) { return this->pointer < other.pointer; }

        template <class U> RefPtr<U> castTo()
        {
            if constexpr (std::is_polymorphic<U>::value) {
#ifndef SC_NO_CHECK
                U* np = dynamic_cast<U*>(pointer);
                return RefPtr<U>(dynamic_cast<U*>(pointer), checkCode, type, offset + ((char*)np - (char*)pointer));
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
            U* np = dynamic_cast<U*>(pointer);
            return RefPtr<U>(np, checkCode, type, offset - ((char*)np - (char*)pointer));
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
            sc_assert(pointer != nullptr, "try access null pointer 0");
            uint32_t* code = (uint32_t*)((char*)pointer + offset);
            sc_assert(checkCode == *code, "try access invalid pointer 1");

            if (type == RefType::ArrayRef) {
                int codeOffset = (int)(&((HeapRefable*)nullptr)->_checkCode);
                HeapRefable* refable = (HeapRefable*)((char*)code - codeOffset);
                int dataOffset = (char*)pointer - (char*)(refable + 1);
                sc_assert(dataOffset < refable->_dataSize, "try access invalid array element pointer 2");
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

        RefPtr(void* p, const uint32_t* checkCodePtr, RefType type) : pointer(p)
#ifndef SC_NO_CHECK
            , checkCode(*checkCodePtr), offset((char*)checkCodePtr - (char*)pointer), type(type)
#endif
        {
        }

        RefPtr(void* p, uint32_t checkCode, RefType type, int offset) : pointer(p)
#ifndef SC_NO_CHECK
            , checkCode(checkCode), offset(offset), type(type)
#endif
        {
        }

        RefPtr(HeapRefable* r) : pointer((void*)(r + 1))
#ifndef SC_NO_CHECK
            , checkCode(r->_checkCode), offset((char*)&(r->_checkCode) - (char*)pointer), type(RefType::HeapRef)
#endif
        {
        }

        template <class U>
        RefPtr(const OwnPtr<U>& p) : pointer(p.get())
        {
#ifndef SC_NO_CHECK
            if (p.isNull()) {
                type = RefType::UnsafeRef;
                checkCode = 0;
                offset = 0;
            }
            else {
                type = RefType::HeapRef;
                auto* checkCopePtr = &(sc_getRefable(pointer)->_checkCode);
                checkCode = *checkCopePtr;
                offset = ((char*)checkCopePtr - (char*)pointer);
            }
#endif
        }

        template <class U>
        RefPtr(const OwnPtr<U>& p, void* ptr) : pointer(ptr) {
#ifndef SC_NO_CHECK
            sc_assert(p.get(), "try access null pointer");
            type = (RefType::HeapRef);
            auto* checkCopePtr = &(sc_getRefable(p.get())->_checkCode);
            checkCode = *checkCopePtr;
            offset = ((char*)checkCopePtr - (char*)pointer);
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
            offset = p.offset + ((char*)pointer - (void*)p.get());
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

        inline bool isNull() const { return pointer == nullptr; }

        bool operator==(const void* other) { return this->pointer == other; }
        bool operator==(const RefPtr<void>& other) { return this->pointer == other.pointer; }
        bool operator<(const RefPtr<void>& other) { return this->pointer < other.pointer; }

        template <class U> RefPtr<U> castTo()
        {
#ifndef SC_NO_CHECK
            return RefPtr<U>((U*)(pointer), checkCode, type, offset);
#else
            return RefPtr<U>((U*)(pointer), 0, RefType::UnsafeRef, 0);
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
        return sric::RefPtr<T>(&b, &b.__checkCode, sric::RefType::IntrusiveRef);
#else
        return sric::RefPtr<T>(&b, 0, sric::RefType::UnsafeRef);
#endif
    }

    template<typename T>
    RefPtr<T> makeRefPtr(T* b) {
#ifndef SC_NO_CHECK
        return sric::RefPtr<T>(b, &b->__checkCode, sric::RefType::IntrusiveRef);
#else
        return sric::RefPtr<T>(b, 0, sric::RefType::UnsafeRef);
#endif
    }

    template <class T>
    RefPtr<T> rawToRef(T* ptr) {
        if constexpr (has_checkcode<T>::value) {
            return addressOf<T>(*ptr);
        }

        HeapRefable* r = sc_getRefable(ptr);
        if (r->_magicCode == SC_HEAP_MAGIC_CODE) {
            return RefPtr<T>(r);
        }

        void* p = toVoid(ptr);
        uint32_t* code = ((uint32_t*)p) - 2;
        if (*code == SC_STACK_MAGIC_CODE) {
            return RefPtr<T>(ptr, (code - 1), RefType::StackRef);
        }

        sc_assert(false, "Can't cast raw pointer to ref pointer");
        return RefPtr<T>(ptr, 0, RefType::UnsafeRef);
    }

}
#endif