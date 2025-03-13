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

//class HeapRefable;
//
//template<typename T>
//class RefPtr;

template<typename U>
typename std::enable_if<std::is_polymorphic<U>::value, HeapRefable*>::type  getRefable(U* pointer) {
    void* mostTop = dynamic_cast<void*>(pointer);
    HeapRefable* p = (HeapRefable*)mostTop;
    --p;
    return p;
}

template<typename U>
typename std::enable_if<!std::is_polymorphic<U>::value, HeapRefable*>::type  getRefable(U* pointer) {
    void* mostTop = (void*)pointer;
    HeapRefable* p = (HeapRefable*)mostTop;
    --p;
    return p;
}

template<typename T>
class OwnPtr {
    T* pointer;
    template <class U> friend class OwnPtr;
    template <class U> friend class SharedPtr;
public:
    OwnPtr() : pointer(nullptr) {
    }

    explicit OwnPtr(T* p) : pointer(p) {
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
            pointer->~T();
            if (p->freeMemory) {
                p->freeMemory(p);
            }
            else {
                free(p);
            }
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

    template <class U> OwnPtr<U> castTo()
    {
        OwnPtr<U> copy((U*)(take()));
        return copy;
    }

    template <class U> OwnPtr<U> dynamicCastTo()
    {
        OwnPtr<U> copy(dynamic_cast<U*>(take()));
        return copy;
    }

    OwnPtr<T> share() {
        if (pointer)
            getRefable(pointer)->addRef();
        return OwnPtr<T>(pointer);
    }
};



template<typename T>
OwnPtr<T> alloc() {
    HeapRefable* p = (HeapRefable*)malloc(sizeof(HeapRefable) + sizeof(T));
    new (p) HeapRefable();
    void* m = (p + 1);
    T* t = new(m) T();
    return OwnPtr<T>(t);
}

template<typename T>
OwnPtr<T> init(void* p, std::function<void(void*)> freeMemory) {
    HeapRefable* h = new (p) HeapRefable();
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
    getRefable(ptr)->addRef();
    return OwnPtr<T>(ptr);
}

}
#endif
