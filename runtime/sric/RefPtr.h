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

#include "sric/OwnPtr.h"
#include "sric/Refable.h"

namespace sric
{

    enum struct RefType : uint8_t
    {
        HeapRef, ArrayRef, StackRef, UnsafeRef, IntrusiveRef, UniqueRef
    };
#ifdef SC_CHECK
    #define sc_onDerefCheck(pointer, offset, type) do {\
            sc_assert(pointer != nullptr, "try access null pointer 0");\
            CheckCodeType* code = (CheckCodeType*)((char*)pointer + offset);\
            sc_assert(checkCode == *code, "try access invalid pointer 1");\
            if (type == RefType::ArrayRef) {\
                HeapRefable* _refable = (HeapRefable*)((char*)code - offsetof(HeapRefable, _checkCode));\
                int dataOffset = -offset + offsetof(HeapRefable, _checkCode) - sizeof(HeapRefable);\
                sc_assert(dataOffset < _refable->_dataSize, "try access invalid array element pointer 2");\
            }\
        } while(false)
#else
    #define sc_onDerefCheck(pointer, offset, type)
#endif

    template<typename T>
    class RefPtr {
        T* pointer;
#ifndef SC_NO_CHECK
        CheckCodeType checkCode;
        RefType type;
        int32_t offset;
        
#endif // !SC_NO_CHECK

        template <class U> friend class RefPtr;
        template <class U> friend RefPtr<U> rawToRef(U* ptr);
        template <class U> friend OwnPtr<U> refToOwn(RefPtr<U> ptr);

    public:

        inline RefPtr() : pointer(nullptr)
#ifndef SC_NO_CHECK
            , checkCode(0), offset(0), type(RefType::UnsafeRef)
#endif
        {
        }

        inline RefPtr(T* p, const CheckCodeType* checkCodePtr, RefType type) : pointer(p)
#ifndef SC_NO_CHECK
            , checkCode(*checkCodePtr), offset((char*)checkCodePtr - (char*)pointer), type(type)
#endif
        {
        }

        inline RefPtr(T* p, CheckCodeType checkCode, RefType type, int offset) : pointer(p)
#ifndef SC_NO_CHECK
            , checkCode(checkCode), offset(offset), type(type)
#endif
        {
        }

        inline RefPtr(StackRefable<T>& p) : pointer(&p.value)
#ifndef SC_NO_CHECK
            , checkCode(p._checkCode), offset((char*)&(p._checkCode) - (char*)pointer), type(RefType::StackRef)
#endif
        {
        }

        inline RefPtr(HeapRefable* r) : pointer((T*)(r + 1))
#ifndef SC_NO_CHECK
            , checkCode(r->_checkCode), offset((char*)&(r->_checkCode) - (char*)pointer), type(RefType::HeapRef)
#endif
        {
        }

        template <class U>
        inline RefPtr(const OwnPtr<U>& p) : pointer(p.get())
        {
#ifndef SC_NO_CHECK
            if (p.isNull()) {
                type = RefType::UnsafeRef;
                checkCode = 0;
                offset = 0;
            }
            else {
                type = RefType::HeapRef;
                CheckCodeType* checkCopePtr = &(sc_getRefable(pointer)->_checkCode);
                checkCode = *checkCopePtr;
                offset = ((char*)checkCopePtr - (char*)pointer);
            }
#endif
        }

        template <class U>
        inline RefPtr(const OwnPtr<U>& p, T* ptr) : pointer(ptr) {
#ifndef SC_NO_CHECK
            sc_assert(p.get(), "try access null pointer");
            type = (RefType::HeapRef);
            CheckCodeType* checkCopePtr = &(sc_getRefable(p.get())->_checkCode);
            checkCode = *checkCopePtr;
            offset = ((char*)checkCopePtr - (char*)pointer);
#endif
        }

        template <class U>
        inline RefPtr(const RefPtr<U>& p) : pointer(p.pointer)
#ifndef SC_NO_CHECK
            , checkCode(p.checkCode), offset(p.offset), type(p.type)
#endif
        {
        }

        template <class U>
        inline RefPtr(const RefPtr<U>& p, T* ptr) : pointer(ptr)
#ifndef SC_NO_CHECK
            , checkCode(p.checkCode), type(p.type)
#endif
        {
#ifndef SC_NO_CHECK
            offset = p.offset + (char*)p.get() - (char*)pointer;
#endif
        }

        inline T* operator->() const {
            sc_onDerefCheck(pointer, offset, type);
            return pointer;
        }

        inline T* operator->() {
            sc_onDerefCheck(pointer, offset, type);
            return pointer;
        }

        inline T& operator*() {
            sc_onDerefCheck(pointer, offset, type);
            return *pointer;
        }

        inline operator T* () { return pointer; }

        inline T* get() const {
#ifdef SC_CHECK
            if (pointer) {
                sc_onDerefCheck(pointer, offset, type);
            }
#endif
            return pointer;
        }

        inline bool isNull() const { return pointer == nullptr; }

        bool operator==(const T* other) { return this->pointer == other; }
        bool operator==(const RefPtr<T> other) { return this->pointer == other.pointer; }
        bool operator<(const RefPtr<T> other) { return this->pointer < other.pointer; }

        template <class U> 
        inline RefPtr<U> castTo()
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

        template <class U> 
        inline RefPtr<U> dynamicCastTo()
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
        CheckCodeType checkCode;
        RefType type;
        int32_t offset;
#endif
        template <class U> friend class RefPtr;
        template <class U> friend RefPtr<U> rawToRef(U* ptr);
        template <class U> friend OwnPtr<U> refToOwn(RefPtr<U> ptr);

    public:

        inline RefPtr() : pointer(nullptr)
#ifndef SC_NO_CHECK
            , checkCode(0), offset(0), type(RefType::UnsafeRef)
#endif
        {
        }

        inline RefPtr(void* p, const CheckCodeType* checkCodePtr, RefType type) : pointer(p)
#ifndef SC_NO_CHECK
            , checkCode(*checkCodePtr), offset((char*)checkCodePtr - (char*)pointer), type(type)
#endif
        {
        }

        inline RefPtr(void* p, CheckCodeType checkCode, RefType type, int offset) : pointer(p)
#ifndef SC_NO_CHECK
            , checkCode(checkCode), offset(offset), type(type)
#endif
        {
        }

        inline RefPtr(HeapRefable* r) : pointer((void*)(r + 1))
#ifndef SC_NO_CHECK
            , checkCode(r->_checkCode), offset((char*)&(r->_checkCode) - (char*)pointer), type(RefType::HeapRef)
#endif
        {
        }

        template <class U>
        inline RefPtr(const OwnPtr<U>& p) : pointer(p.get())
        {
#ifndef SC_NO_CHECK
            if (p.isNull()) {
                type = RefType::UnsafeRef;
                checkCode = 0;
                offset = 0;
            }
            else {
                type = RefType::HeapRef;
                CheckCodeType* checkCopePtr = &(sc_getRefable(pointer)->_checkCode);
                checkCode = *checkCopePtr;
                offset = ((char*)checkCopePtr - (char*)pointer);
            }
#endif
        }

        template <class U>
        inline RefPtr(const OwnPtr<U>& p, void* ptr) : pointer(ptr) {
#ifndef SC_NO_CHECK
            sc_assert(p.get(), "try access null pointer");
            type = (RefType::HeapRef);
            CheckCodeType* checkCopePtr = &(sc_getRefable(p.get())->_checkCode);
            checkCode = *checkCopePtr;
            offset = ((char*)checkCopePtr - (char*)pointer);
#endif
        }

        template <class U>
        inline RefPtr(const RefPtr<U>& p) : pointer(p.pointer)
#ifndef SC_NO_CHECK
            , checkCode(p.checkCode), offset(p.offset), type(p.type)
#endif
        {
        }

        template <class U>
        inline RefPtr(const RefPtr<U>& p, void* ptr) : pointer(ptr)
#ifndef SC_NO_CHECK
            , checkCode(p.checkCode), type(p.type)
#endif
        {
#ifndef SC_NO_CHECK
            offset = p.offset + ((char*)pointer - (char*)p.get());
#endif
        }

        inline void* operator->() const {
            sc_onDerefCheck(pointer, offset, type);
            return pointer;
        }

        inline void* operator->() {
            sc_onDerefCheck(pointer, offset, type);
            return pointer;
        }

        inline operator void* () {
            return pointer;
        }

        inline void* get() const {
#ifdef SC_CHECK
            if (pointer) {
                sc_onDerefCheck(pointer, offset, type);
            }
#endif
            return pointer;
        }

        inline bool isNull() const { return pointer == nullptr; }

        bool operator==(const void* other) { return this->pointer == other; }
        bool operator==(const RefPtr<void> other) { return this->pointer == other.pointer; }
        bool operator<(const RefPtr<void> other) { return this->pointer < other.pointer; }

        template <class U> 
        inline RefPtr<U> castTo()
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
        HeapRefable* r = sc_getRefable(ptr.get());
#ifndef SC_NO_CHECK
        if (r->_magicCode != SC_HEAP_MAGIC_CODE) {
            fprintf(stderr, "ERROR: Can't cast ref pointer to own pointer\n");
            abort();
            return OwnPtr<T>();
        }
#endif
        r->getRefCount()->addRef();
        return OwnPtr<T>(ptr.get());
    }

    template<typename T>
    inline RefPtr<T> addressOf(T& b) {
#ifndef SC_NO_CHECK
        return sric::RefPtr<T>(&b, &b.__checkCode, sric::RefType::IntrusiveRef);
#else
        return sric::RefPtr<T>(&b, 0, sric::RefType::UnsafeRef);
#endif
    }

    template<typename T>
    inline RefPtr<T> makeRefPtr(T* b) {
#ifndef SC_NO_CHECK
        return sric::RefPtr<T>(b, &b->__checkCode, sric::RefType::IntrusiveRef);
#else
        return sric::RefPtr<T>(b, 0, sric::RefType::UnsafeRef);
#endif
    }

    template <class T>
    RefPtr<T> rawToRef(T* ptr) {
#ifndef SC_NO_CHECK
        if constexpr (has_checkcode<T>::value) {
            return addressOf<T>(*ptr);
        }

        HeapRefable* r = sc_getRefable(ptr);
        if (r->_magicCode == SC_HEAP_MAGIC_CODE) {
            return RefPtr<T>(r);
        }

        void* p = toVoid(ptr);
        CheckCodeType* code = ((CheckCodeType*)p) - 2;
        if (*code == SC_STACK_MAGIC_CODE) {
            return RefPtr<T>(ptr, (code - 1), RefType::StackRef);
        }

        sc_assert(false, "Can't cast raw pointer to ref pointer");
        return RefPtr<T>(ptr, 0, RefType::UnsafeRef);
#else
        return RefPtr<T>(ptr, 0, RefType::UnsafeRef);
#endif
    }

}
#endif