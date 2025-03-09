#ifndef _SRIC_DARRAY_H_
#define _SRIC_DARRAY_H_

#include "common.h"
#include <vector>
#include "RefPtr.h"

namespace sric
{
template <typename T>
class DArray : public Noncopyable {
    T* _data = nullptr;
    int _size = 0;
    int _capacity = 0;
public:
    DArray() = default;

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

    void resize(int size) {
        if (size == _size) return;
        tryGrow(size);
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

    RefPtr<T> getRef(int i) {
        T* t = &get(i);
        HeapRefable* p = getHeader();
        return RefPtr<T>(t, p->_checkCode, i * sizeof(T));
    }

    RefPtr<const T> constGetRef(int i) const {
        DArray* self = const_cast<DArray*>(this);
        T* t = &self->get(i);
        HeapRefable* p = self->getHeader();
        return RefPtr<const T>(t, p->_checkCode, i * sizeof(T));
    }

    void set(int i, T& d) {
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
        if (refable->release()) {
            free(refable);
        }
        _data = nullptr;
    }
private:
    void alloc(int bsize) {
        HeapRefable* p;
        if (_data == nullptr) {
            p = (HeapRefable*)malloc(sizeof(HeapRefable) + bsize);
            new (p) HeapRefable();
        }
        else {
            p = getHeader();
            p = (HeapRefable*)realloc(p, sizeof(HeapRefable) + bsize);
            p->_checkCode = generateCheckCode();
        }
        //p->_capacity = bsize;
        _data = (T*)(p + 1);
    }

    HeapRefable* getHeader() {
        return getRefable(_data);
    }

    void tryGrow(int size) {
        if (!_data) {
            alloc(size * sizeof(T));
            return;
        }
        //HeapRefable* refable = getHeader();
        if (_capacity >= size) {
            return;
        }
        int newSize = size * 1.5;
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

    void add(T& d) {
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
        
        (_data + i)->~T();
        int s = size()-i-1;
        memmove(_data + i, _data + i + 1, s * sizeof(T));
        --_size;
        getHeader()->_dataSize -= sizeof(T);
    }

    void reserve(int capacity) {
        tryGrow(capacity);
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
};
}
#endif