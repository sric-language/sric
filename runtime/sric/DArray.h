#ifndef _SRIC_DARRAY_H_
#define _SRIC_DARRAY_H_

#include "sric/common.h"
#include <vector>
#include "sric/RefPtr.h"
#include "sric/util.h"
#include <type_traits>

namespace sric
{

class List {
public:
    virtual ~List() {}
    virtual void clear() = 0;
    virtual void resize(int size) = 0;
    virtual void* data() = 0;
    virtual int size() const = 0;
};

template <typename T>
class DArray : public List, public Noncopyable {

    T* _data = nullptr;
    int _size = 0;
    int _capacity = 0;
public:
    DArray() {}

    DArray(DArray&& other) {
        _data = other._data;
        _size = other._size;
        _capacity = other._capacity;
        other._data = nullptr;
        other._size = 0;
        other._capacity = 0;
    }

    DArray& operator=(DArray&& other) {
        _data = other._data;
        _size = other._size;
        _capacity = other._capacity;
        other._data = nullptr;
        other._size = 0;
        other._capacity = 0;
        return *this;
    }

    void* data() { return _data; }

    void resize(int size) {
        if (size == _size) return;
        tryGrow(size, true);
        if (size > _size) {
            for (int i = _size; i < size; ++i) {
                new(_data + i) T();
            }
        }
        else {
            for (int i = size; i < _size; ++i) {
                (_data + i)->~T();
            }
        }
        _size = size;
#ifndef SC_NO_CHECK
        getHeader()->_dataSize = _size*sizeof(T);
#endif
    }

    T& operator[](int i) {
        sc_assert(i >= 0 && i < size(), "index out of array");
        return _data[i];
    }

    const T& operator[](int i) const {
        sc_assert(i >= 0 && i < size(), "index out of array");
        return _data[i];
    }

    T& get(int i) {
        sc_assert(i >= 0 && i < size(), "index out of array");
        return _data[i];
    }

    T& at(int i) {
        sric::verify(i >= 0 && i < size(), "index out of array");
        return _data[i];
    }

    const T& at(int i) const {
        sric::verify(i >= 0 && i < size(), "index out of array");
        return _data[i];
    }

    const T& get(int i) const {
        sc_assert(i >= 0 && i < size(), "index out of array");
        return _data[i];
    }

    RefPtr<T> getPtr(int i) {
        T* t = &get(i);
#ifndef SC_NO_CHECK
        HeapRefable* p = getHeader();
        return RefPtr<T>(t, &p->_checkCode, RefType::ArrayRef);
#else
        return RefPtr<T>(t, nullptr, RefType::UnsafeRef);
#endif
    }

    RefPtr<const T> getPtr(int i) const {
        DArray* self = const_cast<DArray*>(this);
        T* t = &self->get(i);
#ifndef SC_NO_CHECK
        HeapRefable* p = self->getHeader();
        return RefPtr<const T>(t, &p->_checkCode, RefType::ArrayRef);
#else
        return RefPtr<const T>(t, nullptr, RefType::UnsafeRef);
#endif
    }

    void set(int i, T&& d) {
        sc_assert(i >= 0 && i < size(), "index out of array");
        _data[i] = std::move(d);
    }

    void constSet(int i, const T& d) {
        sc_assert(i >= 0 && i < size(), "index out of array");
        _data[i] = d;
    }

    void clear() {
        if (!_data) return;
        for (int i = 0; i < _size; ++i) {
            (_data + i)->~T();
        }
        _size = 0;
#ifndef SC_NO_CHECK
        getHeader()->_dataSize = 0;
#endif
    }

    ~DArray() {
        if (!_data) return;
        for (int i = 0; i < _size; ++i) {
            (_data + i)->~T();
        }
        _size = 0;
        HeapRefable* refable = getHeader();
        if (!refable->_refCount) {
            refable->~HeapRefable();
            free(refable);
        }
        else {
            if (refable->_refCount->release()) {
                refable->~HeapRefable();
                free(refable);
            }
        }
        _data = nullptr;
    }

    int getCapacity() { return _capacity; }
private:
    void alloc(int bsize) {
        HeapRefable* p;
        if (_data == nullptr) {
            p = (HeapRefable*)malloc(sizeof(HeapRefable) + bsize);
            if (!p) {
                fprintf(stderr, "ERROR: malloc fail\n");
                abort();
            }
            new (p) HeapRefable();
        }
        else {
            if constexpr (std::is_trivially_copyable<T>::value) {
                p = getHeader();
                p = (HeapRefable*)realloc((char*)p, sizeof(HeapRefable) + bsize);
                if (!p) {
                    fprintf(stderr, "ERROR: realloc fail\n");
                    abort();
                }
    #ifndef SC_NO_CHECK
                p->_checkCode = generateCheckCode();
    #endif
            }
            else {
                p = getHeader();
                HeapRefable* np = (HeapRefable*)malloc(sizeof(HeapRefable) + bsize);
                T* ndata = (T*)(np+1);
                //printf("realloc: %p\n", p);

                for (int i = 0; i < _size; ++i) {
                    new (ndata+i) T(std::move(_data[i]));
                    _data[i].~T();
                }
                *np = *p;
#ifndef SC_NO_CHECK
                np->_checkCode = generateCheckCode();
#endif
                free(p);
                p = np;
            }
        }
        //p->_capacity = bsize;
        _data = (T*)(p + 1);
    }

    inline HeapRefable* getHeader() {
        return sc_getRefable(_data);
    }

    void tryGrow(int size, bool definiteSize = false) {
        if (!_data) {
            alloc(size * sizeof(T));
            _capacity = size;
            return;
        }
        //HeapRefable* refable = getHeader();
        if (_capacity >= size) {
            return;
        }
        int newSize = size;
        if (!definiteSize) {
            newSize *= 1.5;
        }
        alloc(newSize * sizeof(T));
        _capacity = newSize;
    }

public:
    void constAdd(const T& d) {
        int pos = size();
        tryGrow(pos + 1);
        T* m = (_data+ pos);
        new(m) T();
        *m = d;
        ++_size;
#ifndef SC_NO_CHECK
        getHeader()->_dataSize += sizeof(T);
#endif
    }

    void add(T d) {
        int pos = size();
        tryGrow(pos + 1);
        T* m = (_data + pos);
        new(m) T();
        *m = std::move(d);
        ++_size;
#ifndef SC_NO_CHECK
        getHeader()->_dataSize += sizeof(T);
#endif
    }

    T pop() {
        if (_size == 0) {
            return T();
        }
        T t = std::move(_data[_size-1]);
        (_data + _size - 1)->~T();
        --_size;
#ifndef SC_NO_CHECK
        getHeader()->_dataSize -= sizeof(T);
#endif
        return t;
    }

    int size() const {
        return _size;
    }

private:
    void moveForword(int dst, int src, int n) {
        int offset = src - dst;
        if constexpr (std::is_trivially_copyable<T>::value) {
            for (int i = 0; i<offset; ++i) {
                (_data + (dst + i))->~T();
            }
            memmove(_data + dst, _data + src, n * sizeof(T));
        }
        else {
            for (int i = 0; i<n; ++i) {
                int to = dst + i;
                int from = src + i;
                _data[to] = std::move(_data[from]);
            }
            for (int i = 0; i<offset; ++i) {
                (_data + (dst + n + i))->~T();
            }
        }
    }
public:
    void removeAt(int i) {
        sc_assert(i >= 0 && i < size(), "index out of array");
        int s = size()-i-1;
        if (s > 0) {
            moveForword(i, i + 1, s);
        }
        else {
            (_data + i)->~T();
        }
        --_size;
#ifndef SC_NO_CHECK
        getHeader()->_dataSize -= sizeof(T);
#endif
    }

    void removeRange(int begin, int end) {
        if (begin >= end) return;
        sc_assert(begin >= 0 && begin < size(), "index out of array");
        sc_assert(end-1 >= 0 && end-1 < size(), "index out of array");

        int s = size()-end;
        if (s > 0) {
            moveForword(begin, end, s);
        }
        else {
            for (int i = begin; i<end; ++i) {
                (_data + i)->~T();
            }
        }
        int n = end - begin;
        _size -= n;
#ifndef SC_NO_CHECK
        getHeader()->_dataSize -= n*sizeof(T);
#endif
    }

    void reserve(int capacity) {
        tryGrow(capacity, true);
    }

    void swap(DArray<T>& o) {
        T* t = _data;
        _data = o._data;
        o._data = t;

        int s = _size;
        _size = o._size;
        o._size = s;

        int c = _capacity;
        _capacity = o._capacity;
        o._capacity = c;
    }

    bool isEmpty() {
        return _size == 0;
    }

private:
    void moveBackword(int dst, int src, int n) {
        int offset = dst - src;
        if constexpr (std::is_trivially_copyable<T>::value) {

            memmove(_data + dst, _data + src, n * sizeof(T));

            for (int i = 0; i < offset; ++i) {
                T* m = _data + (src + i);
                new(m) T();
            }
        }
        else {
            for (int i = 0; i < offset; ++i) {
                T* m = _data + (src + n + i);
                new(m) T();
            }
            for (int i = n-1; i>=0; --i) {
                int to = dst + i;
                int from = src + i;
                _data[to] = std::move(_data[from]);
            }
        }
    }
public:
    void insert(int i, T d) {
        sc_assert(i >= 0 && i <= size(), "index out of array");

        int n = size();
        if (n == i) {
            add(d);
            return;
        }

        tryGrow(n + 1);
        T* m = (_data + i);

        moveBackword(i + 1, i, (n - i));
        //new(m) T();

        *m = std::move(d);
        ++_size;
#ifndef SC_NO_CHECK
        getHeader()->_dataSize += sizeof(T);
#endif
    }

    void insertAll(int i, DArray<T> o) {
        sc_assert(i >= 0 && i <= size(), "index out of array");

        int msize = o.size();
        int n = size();
        tryGrow(n + msize);
        
        if (n > i) {
            moveBackword(i + msize, i, (n - i));
            for (int j = 0; j < msize; ++j) {
                T* m = (_data + i + j);
                *m = std::move(o._data[j]);
            }
        }
        else {
            for (int j = 0; j < msize; ++j) {
                T* m = (_data + i + j);
                new(m) T(std::move(o._data[j]));
            }
        }

        o._size = 0;
        _size += msize;
#ifndef SC_NO_CHECK
        getHeader()->_dataSize += sizeof(T) * msize;
#endif
    }

    DArray<T> copy() const {
        DArray<T> b;
        b.reserve(size());
        for (int i = 0; i < size(); ++i) {
            b.add(sric::copy((*this)[i]));
        }
        return b;
    }
};

}
#endif