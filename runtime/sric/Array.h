#ifndef _SRIC_ARRAY_H_
#define _SRIC_ARRAY_H_

#include "sric/common.h"
#include <utility>

namespace sric
{
template<typename T, int n>
class Array {
    T data[n];
public:
    T& operator[](int i) {
        sc_assert(i >= 0 && i < n, "index out of array");
        return data[i];
    }

    const T& operator[](int i) const {
        sc_assert(i >= 0 && i < n, "index out of array");
        return data[i];
    }

    T* operator&() { return data; }
};

template<typename T>
class ArrayRef {
public:
    T* _data = nullptr;
    int _size = 0;

    void init(T* d, int s) {
        _data = d;
        _size = s;
    }

    T* data() const { return _data; }
    int size() const { return _size; }

    T& operator[](int i) {
        sc_assert(i >= 0 && i < _size, "index out of arrayref");
        return _data[i];
    }

    const T& operator[](int i) const {
        sc_assert(i >= 0 && i < _size, "index out of arrayref");
        return _data[i];
    }

    T& get(int i) {
        sc_assert(i >= 0 && i < _size, "index out of arrayref");
        return _data[i];
    }

    void set(int i, T&& d) {
        sc_assert(i >= 0 && i < _size, "index out of arrayref");
        _data[i] = std::move(d);
    }

    void set(int i, const T& d) {
        sc_assert(i >= 0 && i < _size, "index out of arrayref");
        _data[i] = std::move(d);
    }
};

}
#endif