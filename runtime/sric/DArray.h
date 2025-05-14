#ifndef _SRIC_DARRAY_H_
#define _SRIC_DARRAY_H_

#include "sric/common.h"
#include <vector>
#include "sric/RefPtr.h"
#include "sric/util.h"

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
        getHeader()->_dataSize = _size*sizeof(T);
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

    T& getUnchecked(int i) {
        return _data[i];
    }

    const T& constGet(int i) const {
        sc_assert(i >= 0 && i < size(), "index out of array");
        return _data[i];
    }

    RefPtr<T> getPtr(int i) {
        T* t = &get(i);
        HeapRefable* p = getHeader();
        return RefPtr<T>(t, &p->_checkCode, RefType::ArrayRef);
    }

    RefPtr<const T> constGetPtr(int i) const {
        DArray* self = const_cast<DArray*>(this);
        T* t = &self->get(i);
        HeapRefable* p = self->getHeader();
        return RefPtr<const T>(t, &p->_checkCode, RefType::ArrayRef);
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
        getHeader()->_dataSize = 0;
    }

    ~DArray() {
        if (!_data) return;
        for (int i = 0; i < _size; ++i) {
            (_data + i)->~T();
        }
        _size = 0;
        HeapRefable* refable = getHeader();
        if (!refable->_refCount) {
            freeMemory(refable);
        }
        else {
            if (refable->_refCount->release()) {
                freeMemory(refable);
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
            printf("malloc: %p\n", p);
            new (p) HeapRefable();
        }
        else {
            p = getHeader();
            p = (HeapRefable*)realloc(p, sizeof(HeapRefable) + bsize);
            printf("realloc: %p\n", p);
            p->_checkCode = generateCheckCode();
        }
        //p->_capacity = bsize;
        _data = (T*)(p + 1);
    }

    HeapRefable* getHeader() {
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
        getHeader()->_dataSize += sizeof(T);
    }

    void add(T d) {
        int pos = size();
        tryGrow(pos + 1);
        T* m = (_data + pos);
        new(m) T();
        *m = std::move(d);
        ++_size;
        getHeader()->_dataSize += sizeof(T);
    }

    T pop() {
        if (_size == 0) {
            return T();
        }
        T t = std::move(_data[_size-1]);
        (_data + _size - 1)->~T();
        --_size;
        getHeader()->_dataSize -= sizeof(T);
        return t;
    }

    int size() const {
        return _size;
    }

    void removeAt(int i) {
        sc_assert(i >= 0 && i < size(), "index out of array");

        (_data + i)->~T();
        int s = size()-i-1;
        if (s > 0) {
            memmove(_data + i, _data + i + 1, s * sizeof(T));
        }
        --_size;
        getHeader()->_dataSize -= sizeof(T);
    }

    void removeRange(int begin, int end) {
        if (begin >= end) return;
        sc_assert(begin >= 0 && begin < size(), "index out of array");
        sc_assert(end-1 >= 0 && end-1 < size(), "index out of array");
        int s = size()-end;
        if (s > 0) {
            memmove(_data + begin, _data + end, s * sizeof(T));
        }
        int n = end - begin;
        _size -= n;
        getHeader()->_dataSize -= n*sizeof(T);
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

    void insert(int i, T d) {
        sc_assert(i >= 0 && i <= size(), "index out of array");

        int n = size();
        if (n == i) {
            add(d);
            return;
        }

        tryGrow(n + 1);
        T* m = (_data + i);

        memmove(m + 1, m, (n - i) * sizeof(T));

        new(m) T();
        *m = std::move(d);
        ++_size;
        getHeader()->_dataSize += sizeof(T);
    }

    void insertAll(int i, DArray<T> o) {
        sc_assert(i >= 0 && i <= size(), "index out of array");

        int msize = o.size();
        int n = size();
        tryGrow(n + msize);
        T* m = (_data + i);
        if (n-i > 0) {
            memmove(m + msize, m, (n - i) * sizeof(T));
        }
        for (int i = 0; i < o.size(); ++i) {
            new(m) T();
            *m = std::move(o._data[i]);
            ++m;
        }
        o._size = 0;
        _size += msize;
        getHeader()->_dataSize += sizeof(T) * msize;
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