/*
 * Copyright (c) 2012-2016, chunquedong
 *
 * This file is part of cppfan project
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE version 3.0
 *
 * History:
 *   2012-12-23  Jed Young  Creation
 */
#ifndef _SRIC_AUTOPTR_H_
#define _SRIC_AUTOPTR_H_

#include "sric/RefPtr.h"

namespace sric
{
    template<typename T>
    class AutoMove {
        T val;
    public:
        AutoMove() = default;

        AutoMove(T& other) : val(std::move(other)) {
        }

        AutoMove(T&& other) : val(std::move(other)) {
        }

        AutoMove(AutoMove<T>& other) {
            T& p = (T&)other.val;
            val = std::move(p);
        }

        AutoMove(const AutoMove<T>& other) {
            T& p = (T&)other.val;
            val = std::move(p);
        }

        AutoMove& operator=(AutoMove<T>& other) {
            val = std::move(other.val);
            return *this;
        }

        AutoMove& operator=(const AutoMove<T>& other) {
            val = std::move(other.val);
            return *this;
        }

        T& operator->() { return val; }

        T& operator*() { return val; }

        T& get() const {
            return val;
        }

        void set(AutoMove<T>& p) { val = std::move(p.val); }

        T take() {
            return std::move(val);
        }
    };

    /////////////////////////////////////////////////////////////////////////////////////////////////////

    template<typename T>
    class SharedPtr {
        T* pointer;
        template <class U> friend class SharedPtr;
    public:
        SharedPtr() : pointer(nullptr) {
        }

        inline RefCount* getRefCount() {
            HeapRefable* refp = sc_getRefable(pointer);
            return refp->getRefCount();
        }

        template <class U>
        SharedPtr(OwnPtr<U>& other) : pointer(other.pointer) {
            if (pointer) {
                getRefCount()->addRef();
            }
        }

        template <class U>
        SharedPtr(OwnPtr<U>&& other) : pointer(other.pointer) {
            other.pointer = nullptr;
        }

        SharedPtr(SharedPtr& other) : pointer(other.pointer) {
            if (pointer) {
                getRefCount()->addRef();
            }
        }

        template <class U>
        SharedPtr(SharedPtr<U>& other) : pointer(other.pointer) {
            if (pointer) {
                getRefCount()->addRef();
            }
        }

        SharedPtr(SharedPtr&& other) : pointer(other.pointer) {
            other.pointer = nullptr;
        }

        SharedPtr(const SharedPtr& other) : pointer(other.pointer) {
            if (pointer) {
                getRefCount()->addRef();
            }
        }

        template <class U>
        SharedPtr(const SharedPtr<U>& other) : pointer(other.pointer) {
            if (pointer) {
                getRefCount()->addRef();
            }
        }

        SharedPtr(const SharedPtr&& other) : pointer(other.pointer) {
            if (pointer) {
                getRefCount()->addRef();
            }
        }

        virtual ~SharedPtr() {
            clear();
        }

        SharedPtr& operator=(SharedPtr& other) {
            if (other.pointer) {
                other.getRefCount()->addRef();
            }
            if (pointer) {
                dealloc(pointer);
            }
            pointer = other.pointer;
            return *this;
        }

        SharedPtr& operator=(SharedPtr&& other) {
            if (pointer) {
                dealloc(pointer);
            }
            pointer = other.pointer;
            other.pointer = nullptr;
            return *this;
        }

        SharedPtr& operator=(const SharedPtr& other) {
            if (other.pointer) {
                other.getRefCount()->addRef();
            }
            if (pointer) {
                dealloc(pointer);
            }
            pointer = other.pointer;
            return *this;
        }

        SharedPtr& operator=(const SharedPtr&& other) {
            if (other.pointer) {
                other.getRefCount()->addRef();
            }
            if (pointer) {
                dealloc(pointer);
            }
            pointer = other.pointer;
            return *this;
        }

        inline T* operator->() { return pointer; }

        T& operator*() { return *pointer; }

        bool operator==(const SharedPtr& other) { return this->pointer == other.pointer; }
        bool operator!=(const SharedPtr& other) { return this->pointer != other.pointer; }

        void _set(T* p) { pointer = p; }

        template <class U>
        void set(OwnPtr<U>& other) {
            if (other.pointer) {
                other.getRefCount()->addRef();
            }
            if (pointer) {
                dealloc(pointer);
            }
            pointer = other.pointer;
        }

        OwnPtr<T> getOwn() {
            if (!pointer) {
                return OwnPtr<T>();
            }
            getRefCount()->addRef();
            return OwnPtr<T>((T*)(pointer));
        }

        inline RefPtr<T> getPtr() {
            if (!pointer) {
                return RefPtr<T>();
            }
            HeapRefable* refp = sc_getRefable(pointer);
            return RefPtr<T>(refp);
        }

        inline T& get() {
            sc_assert(pointer, "try deref null pointer");
            return *pointer;
        }

        bool isNull() const { return pointer == nullptr; }

        void clear() {
            if (pointer) {
                dealloc(pointer);
                pointer = nullptr;
            }
        }

        T* take() {
            T* p = pointer;
            pointer = nullptr;
            return p;
        }

    };

    /////////////////////////////////////////////////////////////////////////////////////////////////////

    template<typename T>
    class WeakPtr {
        RefCount* pointer;
    public:
        WeakPtr() : pointer(NULL) {
        }

        WeakPtr(OwnPtr<T>& p) : pointer(NULL) {
            HeapRefable* refp = sc_getRefable(p.get());
            if (refp) {
                pointer = refp->getRefCount();
                pointer->addWeakRef();
            }
        }

        void init(OwnPtr<T>& p) {
            if (pointer) {
                pointer->releaseWeak();
            }

            HeapRefable* refp = sc_getRefable(p.get());
            if (refp) {
                pointer = refp->getRefCount();
                pointer->addWeakRef();
            }
        }

        /*WeakPtr(T* other) : pointer(NULL) {
            if (other) {
                HeapRefable* refp = sc_getRefable(other);
                pointer = refp->getRefCount();
                pointer->addWeakRef();
            }
        }*/

        WeakPtr(const WeakPtr& other) : pointer(other.pointer) {
            if (other.pointer) {
                other.pointer->addWeakRef();
            }
        }

        WeakPtr(WeakPtr&& other) : pointer(other.pointer) {
            other.pointer = nullptr;
        }

        virtual ~WeakPtr() {
            clear();
        }

        WeakPtr& operator=(const WeakPtr& other) {
            if (other.pointer) {
                other.pointer->addWeakRef();
            }
            if (pointer) {
                pointer->releaseWeak();
            }
            pointer = other.pointer;
            return *this;
        }

        WeakPtr& operator=(WeakPtr&& other) {
            if (pointer) {
                pointer->releaseWeak();
            }
            pointer = other.pointer;
            other.pointer = nullptr;
            return *this;
        }

        OwnPtr<T> lock() {
            if (!pointer) {
                return OwnPtr<T>();
            }
            if (!pointer->lock()) {
                return OwnPtr<T>();
            }
            HeapRefable* refp = pointer->_pointer;
            return OwnPtr<T>((T*)(refp + 1));
        }

        void clear() {
            if (pointer) {
                pointer->releaseWeak();
                pointer = nullptr;
            }
        }
    };

    /////////////////////////////////////////////////////////////////////////////////////////////////////
    template<typename T>
    SharedPtr<T> toShared(OwnPtr<T>& t) {
        return SharedPtr<T>(t);
    }

    template<typename T>
    WeakPtr<T> toWeak(OwnPtr<T>& t) {
        return WeakPtr<T>(t);
    }

    template<typename T>
    SharedPtr<T> toShared(OwnPtr<T>&& t) {
        return SharedPtr<T>(t);
    }

    template<typename T>
    WeakPtr<T> toWeak(OwnPtr<T>&& t) {
        return WeakPtr<T>(t);
    }

    template<typename T>
    AutoMove<T> autoMove(T p) {
        return AutoMove<T>(std::move(p));
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////

    template<typename T>
    class UniquePtr {
        T* pointer;
        template<typename U, typename... Args> friend UniquePtr<U> makeUnique(Args&&... args);

        inline explicit UniquePtr(T* p) : pointer(p) {
        }
    public:
        inline UniquePtr() : pointer(nullptr) {
        }

        inline ~UniquePtr() {
            clear();
        }

        inline UniquePtr(const UniquePtr& other) = delete;

        template <class U>
        inline UniquePtr(UniquePtr<U>&& other) : pointer(other.pointer) {
            other.pointer = nullptr;
        }

        UniquePtr& operator=(const UniquePtr& other) = delete;

        inline UniquePtr& operator=(UniquePtr&& other) {
            if (pointer) {
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

        inline T* getRaw() const { return pointer; }

        inline RefPtr<T> getPtr() {
#ifdef SC_NO_CHECK
            return RefPtr<T>(pointer, nullptr, RefType::UnsafeRef);
#else
            if (!pointer) {
                return RefPtr<T>(pointer, nullptr, RefType::UnsafeRef);
            }
            uint64_t* p = (uint64_t*)toVoid(pointer) - 1;
            return RefPtr<T>(pointer, (CheckCodeType*)p, RefType::UniqueRef);
#endif
        }

        inline T& get() {
            sc_assert(pointer, "try deref null pointer");
            return *pointer;
        }

        inline bool isNull() const { return pointer == nullptr; }

        inline void clear() {
            if (pointer) {
                doFree(pointer);
                pointer = nullptr;
            }
        }

    private:
        inline void doFree(T* pointer) {
#ifdef SC_NO_CHECK
            this->pointer->~T();
            free(pointer);
#else
            uint64_t* p = (uint64_t*)toVoid(pointer) - 1;
            this->pointer->~T();
            *p = 0;
            free(p);
#endif
        }
    public:
        T* take() {
            T* p = pointer;
            pointer = nullptr;
            return p;
        }

        void swap(UniquePtr& other) {
            T* p = pointer;
            pointer = other.pointer;
            other.pointer = p;
        }
    };

    template<typename T, typename... Args>
    UniquePtr<T> makeUnique(Args&&... args) {
#ifdef SC_NO_CHECK
        void* p = (void*)malloc(sizeof(T));
        if (!p) {
            fprintf(stderr, "ERROR: alloc memory fail\n");
            return UniquePtr<T>();
        }
        T* t = new(p) T(std::forward<Args>(args)...);
        return UniquePtr<T>(t);
#else
        uint64_t* p = (uint64_t*)malloc(sizeof(uint64_t) + sizeof(T));
        if (!p) {
            fprintf(stderr, "ERROR: alloc memory fail\n");
            return UniquePtr<T>();
        }
        //printf("malloc: %p\n", p);
        *((CheckCodeType*)p) = generateCheckCode();
        void* m = (p + 1);
        T* t = new(m) T(std::forward<Args>(args)...);
        return UniquePtr<T>(t);
#endif
    }
}
#endif
