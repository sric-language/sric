/*
 * Copyright (c) 2012-2016, chunquedong
 *
 * This file is part of cppfan project
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE version 3.0
 *
 * History:
 *   2012-12-23  Jed Young  Creation
 */
#ifndef _SRIC_UNIQPTR_H_
#define _SRIC_UNIQPTR_H_

#include "sric/Refable.h"
#include "sric/common.h"

#define sc_getUniqRefable(pointer) (((UniqRefable*)toVoid(pointer))-1)

namespace sric
{
    template<typename T>
    class UniquePtr {
        T* pointer;
        template<typename U, typename... Args> friend UniquePtr<U> makeUniq(Args&&... args);

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

        inline T* get() const { return pointer; }

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
            void* p = toVoid(pointer);
            this->pointer->~T();
            free(p);
#else
            UniqRefable* p = sc_getUniqRefable(pointer);
            this->pointer->~T();
            p->~UniqRefable();
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
    UniquePtr<T> makeUniq(Args&&... args) {
#ifdef SC_NO_CHECK
        void* p = (void*)malloc(sizeof(T));
        if (!p) {
            fprintf(stderr, "ERROR: alloc memory fail\n");
            return UniquePtr<T>();
        }
        T* t = new(p) T(std::forward<Args>(args)...);
        return UniquePtr<T>(t);
#else
        UniqRefable* p = (UniqRefable*)malloc(sizeof(UniqRefable) + sizeof(T));
        if (!p) {
            fprintf(stderr, "ERROR: alloc memory fail\n");
            return UniquePtr<T>();
        }
        new (p) UniqRefable;
        void* m = (p + 1);
        T* t = new(m) T(std::forward<Args>(args)...);
        return UniquePtr<T>(t);
#endif
    }

    template <class T>
    inline T* takeOwn(UniquePtr<T> p) {
        return p.take();
    }
}
#endif
