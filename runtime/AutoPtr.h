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

#include "RefPtr.h"

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

        //T take() const {
        //    return std::move(val);
        //}
    };

    /////////////////////////////////////////////////////////////////////////////////////////////////////

    template<typename T>
    class SharedPtr {
        T* pointer;
        template <class U> friend class SharedPtr;
    public:
        SharedPtr() : pointer(nullptr) {
        }

        SharedPtr(const SharedPtr& other) : pointer(other.pointer) {
            if (other.pointer) {
                HeapRefable* refp = getRefable(other.pointer);
                refp->addRef();
            }
        }

        template <class U>
        SharedPtr(SharedPtr<U>& other) : pointer(other.pointer) {
            if (other.pointer) {
                HeapRefable* refp = getRefable(other.pointer);
                refp->addRef();
            }
        }

        virtual ~SharedPtr() {
            clear();
        }

        SharedPtr& operator=(T* other) {
            if (other) {
                HeapRefable* refp = getRefable(other);
                refp->addRef();
            }
            if (pointer) {
                HeapRefable* refp = getRefable(pointer);
                refp->release();
            }
            pointer = other;
            return *this;
        }

        SharedPtr& operator=(const SharedPtr& other) {
            if (other.pointer) {
                HeapRefable* refp = getRefable(other.pointer);
                refp->addRef();
            }
            if (pointer) {
                HeapRefable* refp = getRefable(pointer);
                refp->release();
            }
            pointer = other.pointer;
            return *this;
        }

        T* operator->() { return pointer; }

        T& operator*() { return *pointer; }

        bool operator==(const SharedPtr& other) { return this->pointer == other.pointer; }
        bool operator!=(const SharedPtr& other) { return this->pointer != other.pointer; }

        void _set(T* p) { pointer = p; }

        template <class U>
        void set(OwnPtr<U>& other) {
            if (other.pointer) {
                HeapRefable* refp = getRefable(other.pointer);
                refp->addRef();
            }
            if (pointer) {
                HeapRefable* refp = getRefable(pointer);
                refp->release();
            }
            pointer = other.pointer;
        }

        OwnPtr<T> get() {
            if (!pointer) {
                return OwnPtr<T>();
            }
            getRefable(pointer)->addRef();
            return OwnPtr<T>((T*)(pointer));
        }

        bool isNull() const { return pointer == nullptr; }

        void clear() {
            if (pointer) {
                HeapRefable* refp = getRefable(pointer);
                refp->release();
                pointer = nullptr;
            }
        }

        T* take() {
            if (pointer) {
                HeapRefable* refp = getRefable(pointer);
                refp->addRef();
            }
            return pointer;
        }
    };

    /////////////////////////////////////////////////////////////////////////////////////////////////////

    template<typename T>
    class WeakPtr {
        WeakRefBlock* pointer;
    public:
        WeakPtr() : pointer(NULL) {
        }

        WeakPtr(OwnPtr<T>& p) : pointer(NULL) {
            HeapRefable* refp = getRefable(p.get());
            if (refp) {
                pointer = refp->getWeakRefBlock();
                pointer->addRef();
            }
        }

        void init(OwnPtr<T>& p) {
            if (pointer) {
                pointer->release();
            }

            HeapRefable* refp = getRefable(p.get());
            if (refp) {
                pointer = refp->getWeakRefBlock();
                pointer->addRef();
            }
        }

        WeakPtr(T* other) : pointer(NULL) {
            if (other) {
                HeapRefable* refp = getRefable(other);
                pointer = refp->getWeakRefBlock();
                pointer->addRef();
            }
        }

        WeakPtr(const WeakPtr& other) : pointer(other.pointer) {
            if (other.pointer) {
                other.pointer->addRef();
            }
        }

        virtual ~WeakPtr() {
            clear();
        }

        WeakPtr& operator=(const WeakPtr& other) {
            if (other.pointer) {
                other.pointer->addRef();
            }
            if (pointer) {
                pointer->release();
            }
            pointer = other.pointer;
            return *this;
        }

        OwnPtr<T> lock() {
            if (!pointer) {
                return OwnPtr<T>();
            }
            HeapRefable* refp = pointer->lock();
            if (!refp) {
                return OwnPtr<T>();
            }
            return OwnPtr<T>((T*)(refp + 1));
        }

        void clear() {
            if (pointer) {
                pointer->release();
                pointer = nullptr;
            }
        }
    };

    /////////////////////////////////////////////////////////////////////////////////////////////////////

    template<typename T>
    SharedPtr<T> toShared(OwnPtr<T> t) {
        return SharedPtr<T>(t);
    }

    template<typename T>
    WeakPtr<T> toWeak(OwnPtr<T> t) {
        return WeakPtr<T>(t);
    }

    template<typename T>
    AutoMove<T> autoMove(T p) {
        return AutoMove<T>(std::move(p));
    }
}
#endif
