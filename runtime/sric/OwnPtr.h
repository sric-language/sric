/*
 * Copyright (c) 2012-2016, chunquedong
 *
 * This file is part of cppfan project
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE version 3.0
 *
 * History:
 *   2012-12-23  Jed Young  Creation
 */
#ifndef _SRIC_OWNPTR_H_
#define _SRIC_OWNPTR_H_

#include <cstdio>
#include <cstdlib>
#include <type_traits>
#include <functional>

#include "sric/Refable.h"
#include "sric/common.h"

namespace sric
{

#define sc_getRefable(pointer) (((HeapRefable*)toVoid(pointer))-1)

inline void freeMemory(void* p) {
    //printf("free: %p\n", p);
    free(p);
}

template<typename T>
class RefPtr;

template<typename T>
class OwnPtr {
    T* pointer;
    template <class U> friend class OwnPtr;
    template <class U> friend class SharedPtr;
    template <class U> friend class WeakPtr;
    template<typename U, typename... Args> friend OwnPtr<U> new_(Args&&... args);
    template <class U> friend OwnPtr<U> rawToOwn(U* ptr);
    template <class U> friend OwnPtr<U> refToOwn(RefPtr<U> ptr);

    inline explicit OwnPtr(T* p) : pointer(p) {
    }
public:
    inline OwnPtr() : pointer(nullptr) {
    }

    inline ~OwnPtr() {
        clear();
    }

    inline OwnPtr(const OwnPtr& other) = delete;

    template <class U>
    inline OwnPtr(OwnPtr<U>&& other) : pointer(other.pointer) {
        other.pointer = nullptr;
    }

    OwnPtr& operator=(const OwnPtr& other) = delete;

    inline OwnPtr& operator=(OwnPtr&& other) {
        if (pointer != other.pointer && pointer) {
            doFree(pointer);
        }
        pointer = other.pointer;
        other.pointer = nullptr;

        return *this;
    }

    inline T* operator->() const { sc_assert(pointer != nullptr, "try deref null pointer"); return pointer; }

    inline T* operator->() { sc_assert(pointer != nullptr, "try deref null pointer"); return pointer; }

    inline T& operator*() { sc_assert(pointer != nullptr, "try deref null pointer"); return *pointer; }

    inline const T& operator*() const { sc_assert(pointer != nullptr, "try deref null pointer"); return *pointer; }

    inline operator T* () const { return pointer; }
    inline operator T* () { return pointer; }

    inline T* get() const { return pointer; }

    inline bool isNull() const { return pointer == nullptr; }

    inline void clear() {
        if (pointer) {
            doFree(pointer);
            pointer = nullptr;
        }
    }

private:
    void doFree(T* pointer) {
        HeapRefable* p = sc_getRefable(pointer);
        if (!p->_refCount) {
            this->pointer->~T();
            p->~HeapRefable();
            freeMemory(p);
        }
        else {
            //void (*custemfreeMemory)(void*) = p->_refCount->freeMemory;
            if (p->_refCount->release()) {
                this->pointer->~T();
                p->~HeapRefable();
                freeMemory(p);
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

    OwnPtr<T> share() {
        if (pointer)
            sc_getRefable(pointer)->getRefCount()->addRef();
        return OwnPtr<T>(pointer);
    }
};

template <typename T>
void callDestructor(void* p) {
    ((T*)p)->~T();
}

template<>
class OwnPtr<void> {
    void* pointer;

    template <class U> friend class OwnPtr;
    template <class U> friend class SharedPtr;
    template <class U> friend class WeakPtr;
    template<typename U, typename... Args> friend OwnPtr<U> new_(Args&&... args);
    template <class U> friend OwnPtr<U> rawToOwn(U* ptr);
    template <class U> friend OwnPtr<U> refToOwn(RefPtr<U> ptr);

    inline explicit OwnPtr(void* p) : pointer(p) {
    }
public:
    inline OwnPtr() : pointer(nullptr) {
    }

    inline ~OwnPtr() {
        clear();
    }

    inline OwnPtr(const OwnPtr& other) = delete;

    inline OwnPtr(OwnPtr<void>&& other) : pointer(other.pointer) {
        other.pointer = nullptr;
    }

    template <class U>
    inline OwnPtr(OwnPtr<U>&& other) : pointer(other.pointer) {
        other.pointer = nullptr;
    }

    OwnPtr& operator=(const OwnPtr& other) = delete;

    inline OwnPtr& operator=(OwnPtr<void>&& other) {
        if (pointer != other.pointer && pointer) {
            doFree(pointer);
        }
        pointer = other.pointer;
        other.pointer = nullptr;

        return *this;
    }

    template <class U>
    inline OwnPtr& operator=(OwnPtr<U>&& other) {
        if (pointer != other.pointer && pointer) {
            doFree(pointer);
        }
        pointer = other.pointer;
        other.pointer = nullptr;
        return *this;
    }

    inline void* operator->() const { sc_assert(pointer != nullptr, "try deref null pointer"); return pointer; }

    inline void* operator->() { sc_assert(pointer != nullptr, "try deref null pointer"); return pointer; }

    // T& operator*() { sc_assert(pointer != nullptr, "try deref null pointer"); return *pointer; }

    // const T& operator*() const { sc_assert(pointer != nullptr, "try deref null pointer"); return *pointer; }

    operator void* () { return pointer; }

    operator void* () const { return pointer; }

    inline void* get() const { return pointer; }

    inline bool isNull() const { return pointer == nullptr; }

    inline void clear() {
        if (pointer) {
            doFree(pointer);
            pointer = nullptr;
        }
    }

private:
    void doFree(void* pointer) {
        HeapRefable* p = sc_getRefable(pointer);
        if (!p->_refCount) {
            p->destructor(this->pointer);
            p->~HeapRefable();
            freeMemory(p);
        }
        else {
            //void (*custemFreeMemory)(void*) = p->_refCount->freeMemory;
            if (p->_refCount->release()) {
                p->destructor(this->pointer);
                p->~HeapRefable();
                freeMemory(p);
            }
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

    OwnPtr<void> share() {
        if (pointer)
            sc_getRefable(pointer)->getRefCount()->addRef();
        return OwnPtr<void>(pointer);
    }
};


template<typename T, typename... Args>
OwnPtr<T> new_(Args&&... args) {
    //static_assert(sizeof(HeapRefable) % 8 == 0, "HeapRefable must be 8-byte aligned");

    HeapRefable* p = (HeapRefable*)malloc(sizeof(HeapRefable) + sizeof(T));
    if (!p) {
        fprintf(stderr, "ERROR: alloc memory fail\n");
        return OwnPtr<T>();
    }
    //printf("malloc: %p\n", p);
    new (p) HeapRefable();
    p->destructor = callDestructor<T>;
    void* m = (p + 1);
    T* t = new(m) T(std::forward<Args>(args)...);
    return OwnPtr<T>(t);
}

template<typename T, typename... Args>
OwnPtr<void> newVoid(Args&&... args) {
    OwnPtr<T> t = new_<T>(std::forward<Args>(args)...);
    return t;
}

template<typename T, typename... Args>
OwnPtr<T> makePtr(Args&&... args) {
    return new_<T>(std::forward<Args>(args)...);
}

template<typename T, typename... Args>
T makeValue(Args&&... args) {
    return T(std::forward<Args>(args)...);
}

//template<typename T>
//OwnPtr<T> placementNew(void* p, std::function<void(void*)> freeMemory) {
//    HeapRefable* h = new (p) HeapRefable();
//    h->getRefCount()->freeMemory = freeMemory.target<void (void*)>();
//    void* m = ((HeapRefable*)p + 1);
//    T* t = new(m) T();
//    return OwnPtr<T>(t);
//}

template <class T>
inline OwnPtr<T> share(OwnPtr<T>& p) {
    return p.share();
}

template <class T>
OwnPtr<T> rawToOwn(T* ptr) {
    HeapRefable *r = sc_getRefable(ptr);
#ifndef SC_NO_CHECK
    if (r->_magicCode != SC_HEAP_MAGIC_CODE) {
        fprintf(stderr, "ERROR: try cast raw pointer to own ptr: %p\n", ptr);
        abort();
    }
#endif
    r->getRefCount()->addRef();
    return OwnPtr<T>(ptr);
}

template <class T>
inline T* takeOwn(OwnPtr<T> p) {
    return p.take();
}


}
#endif
