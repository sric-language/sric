/*
 * Copyright (c) 2012-2016, chunquedong
 *
 * This file is part of cppfan project
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE version 3.0
 *
 * History:
 *   2012-12-23  Jed Young  Creation
 */
#ifndef _SRIC_PTR_H_
#define _SRIC_PTR_H_

#include <cstdio>
#include <cstdlib>
#include <type_traits>
#include <functional>

#include "Refable.h"
#include "common.h"

namespace sric
{

template<typename U>
typename std::enable_if<std::is_polymorphic<U>::value, void*>::type  toVoid(U* pointer) {
    return dynamic_cast<void*>(pointer);
}

template<typename U>
typename std::enable_if<!std::is_polymorphic<U>::value, void*>::type  toVoid(U* pointer) {
    return (void*)pointer;
}

template<typename U>
HeapRefable* getRefable(U* pointer) {
    void* mostTop = toVoid(pointer);
    HeapRefable* p = (HeapRefable*)mostTop;
    --p;
    return p;
}

template<typename T>
class RefPtr;

template<typename T>
class OwnPtr {
    T* pointer;
    template <class U> friend class OwnPtr;
    template <class U> friend class SharedPtr;
    template <class U> friend class WeakPtr;
    template <class U> friend OwnPtr<U> new_();
    template <class U> friend OwnPtr<U> rawToOwn(U* ptr);
    template <class U> friend OwnPtr<U> refToOwn(RefPtr<U> ptr);

    explicit OwnPtr(T* p) : pointer(p) {
    }
public:
    OwnPtr() : pointer(nullptr) {
    }

    ~OwnPtr() {
        clear();
    }

    OwnPtr(const OwnPtr& other) = delete;

    OwnPtr(OwnPtr&& other) {
        if (other.pointer) {
            pointer = other.pointer;
            other.pointer = nullptr;
        }
        else {
            pointer = nullptr;
        }
    }

    template <class U>
    OwnPtr(OwnPtr<U>&& other) {
        if (other.pointer) {
            pointer = other.pointer;
            other.pointer = nullptr;
        }
        else {
            pointer = nullptr;
        }
    }

    OwnPtr& operator=(const OwnPtr& other) = delete;

    OwnPtr& operator=(OwnPtr&& other) {
        T* toDelete = pointer;

        if (other.pointer) {
            pointer = other.pointer;
            other.pointer = nullptr;
        }
        else {
            pointer = nullptr;
        }

        if (toDelete) {
            doFree(toDelete);
        }
        return *this;
    }

    T* operator->() const { sc_assert(pointer != nullptr, "try deref null pointer"); return pointer; }

    T* operator->() { sc_assert(pointer != nullptr, "try deref null pointer"); return pointer; }

    T& operator*() { sc_assert(pointer != nullptr, "try deref null pointer"); return *pointer; }

    const T& operator*() const { sc_assert(pointer != nullptr, "try deref null pointer"); return *pointer; }

    operator T* () { return pointer; }

    //template <class U>
    //operator RefPtr<U>() { return RefPtr<U>(this); }
    //operator RefPtr<T>() { return RefPtr<T>(this); }

    T* get() const { return pointer; }

    bool isNull() const { return pointer == nullptr; }

    void clear() {
        if (pointer) {
            doFree(pointer);
            pointer = nullptr;
        }
    }

private:
    void doFree(T* pointer) {
        HeapRefable* p = getRefable(pointer);
        if (p->release()) {
            p->dealloc(p);
        }
    }
public:
    T* take() {
        T* p = pointer;
        pointer = nullptr;
        return p;
    }

    void swap(OwnPtr& other) {
        T* p = pointer;
        pointer = other.pointer;
        other.pointer = p;
    }

    //template <class U> OwnPtr<U> castTo()
    //{
    //    OwnPtr<U> copy((U*)(take()));
    //    return copy;
    //}

    //template <class U> OwnPtr<U> dynamicCastTo()
    //{
    //    OwnPtr<U> copy(dynamic_cast<U*>(take()));
    //    return copy;
    //}

    OwnPtr<T> share() {
        if (pointer)
            getRefable(pointer)->addRef();
        return OwnPtr<T>(pointer);
    }
};


template<>
class OwnPtr<void> {
    void* pointer;
    template <class U> friend class OwnPtr;
    template <class U> friend class SharedPtr;
    template <class U> friend class WeakPtr;
    template <class U> friend OwnPtr<U> new_();
    template <class U> friend OwnPtr<U> rawToOwn(U* ptr);
    template <class U> friend OwnPtr<U> refToOwn(RefPtr<U> ptr);

    explicit OwnPtr(void* p) : pointer(p) {
    }
public:
    OwnPtr() : pointer(nullptr) {
    }


    ~OwnPtr() {
        clear();
    }

    OwnPtr(const OwnPtr& other) = delete;

    OwnPtr(OwnPtr&& other) {
        if (other.pointer) {
            pointer = other.pointer;
            other.pointer = nullptr;
        }
        else {
            pointer = nullptr;
        }
    }

    template <class U>
    OwnPtr(OwnPtr<U>&& other) {
        if (other.pointer) {
            pointer = other.pointer;
            other.pointer = nullptr;
        }
        else {
            pointer = nullptr;
        }
    }

    OwnPtr& operator=(const OwnPtr& other) = delete;

    OwnPtr& operator=(OwnPtr&& other) {
        void* toDelete = pointer;

        if (other.pointer) {
            pointer = other.pointer;
            other.pointer = nullptr;
        }
        else {
            pointer = nullptr;
        }

        if (toDelete) {
            doFree(toDelete);
        }
        return *this;
    }

    void* operator->() const { sc_assert(pointer != nullptr, "try deref null pointer"); return pointer; }

    void* operator->() { sc_assert(pointer != nullptr, "try deref null pointer"); return pointer; }

    // T& operator*() { sc_assert(pointer != nullptr, "try deref null pointer"); return *pointer; }

    // const T& operator*() const { sc_assert(pointer != nullptr, "try deref null pointer"); return *pointer; }

    operator void* () { return pointer; }

    //template <class U>
    //operator RefPtr<U>() { return RefPtr<U>(this); }
    //operator RefPtr<T>() { return RefPtr<T>(this); }

    void* get() const { return pointer; }

    bool isNull() const { return pointer == nullptr; }

    void clear() {
        if (pointer) {
            doFree(pointer);
            pointer = nullptr;
        }
    }

private:
    void doFree(void* pointer) {
        HeapRefable* p = getRefable(pointer);
        if (p->release()) {
            p->dealloc(p);
        }
    }
public:
    void* take() {
        void* p = pointer;
        pointer = nullptr;
        return p;
    }

    void swap(OwnPtr& other) {
        void* p = pointer;
        pointer = other.pointer;
        other.pointer = p;
    }

    //template <class U> OwnPtr<U> castTo()
    //{
    //    OwnPtr<U> copy((U*)(take()));
    //    return copy;
    //}

    //template <class U> OwnPtr<U> dynamicCastTo()
    //{
    //    OwnPtr<U> copy(dynamic_cast<U*>(take()));
    //    return copy;
    //}

    OwnPtr<void> share() {
        if (pointer)
            getRefable(pointer)->addRef();
        return OwnPtr<void>(pointer);
    }
};



template<typename T>
void dealloc(HeapRefable* p) {
    void* apointer = p + 1;
    T* pointer = (T*)(apointer);
    pointer->~T();
    if (p->freeMemory) {
        p->freeMemory(p);
    }
}

template<typename T>
OwnPtr<T> new_() {
    //static_assert(alignof(HeapRefable) == 8, "HeapRefable must be 8-byte aligned");

    HeapRefable* p = (HeapRefable*)malloc(sizeof(HeapRefable) + sizeof(T));
    new (p) HeapRefable();
    p->dealloc = dealloc<T>;
    p->freeMemory = free;
    void* m = (p + 1);
    T* t = new(m) T();
    return OwnPtr<T>(t);
}

template<typename T>
OwnPtr<T> placementNew(void* p, std::function<void(void*)> freeMemory) {
    HeapRefable* h = new (p) HeapRefable();
    h->dealloc = dealloc<T>;
    h->freeMemory = freeMemory.target<void (void*)>();
    void* m = ((HeapRefable*)p + 1);
    T* t = new(m) T();
    return OwnPtr<T>(t);
}

template <class T>
OwnPtr<T> share(OwnPtr<T>& p) {
    return p.share();
}

template <class T>
OwnPtr<T> rawToOwn(T* ptr) {
    HeapRefable *r = getRefable(ptr);
    if (r->_magicCode != SC_HEAP_MAGIC_CODE) {
        fprintf(stderr, "ERROR: try cast raw pointer to own ptr: %p\n", ptr);
        abort();
    }
    r->addRef();
    return OwnPtr<T>(ptr);
}

}
#endif
