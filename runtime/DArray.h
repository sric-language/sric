#ifndef _SRIC_DARRAY_H_
#define _SRIC_DARRAY_H_

#include "common.h"
#include <vector>
#include "Ptr.h"

namespace sric
{
template <typename T>
class DArray : public Noncopyable {
    std::vector<T> data;
public:
    void resize(int size) {
        data.resize(size);
    }

    T& operator[](int i) {
        sc_assert(i >= 0 && i < data.size(), "index out of array");
        return data[i];
    }

    const T& operator[](int i) const {
        sc_assert(i >= 0 && i < data.size(), "index out of array");
        return data[i];
    }

    sric::RefPtr<T> get(int i) {
        sc_assert(i >= 0 && i < data.size(), "index out of array");
        return RefPtr<T>(&data[i]);
    }

    const sric::RefPtr<const T> constGet(int i) const {
        sc_assert(i >= 0 && i < data.size(), "index out of array");
        return RefPtr<const T>(&data[i]);
    }

    void set(int i, const T& d) {
        sc_assert(i >= 0 && i < data.size(), "index out of array");
        data[i] = d;
    }

    //void add(T d) {
    //    data.push_back(d);
    //}

    void add(const T& d) {
        data.push_back(std::move(d));
    }

    int size() const {
        return data.size();
    }

    void removeAt(int i) {
        data.erase(data.begin() + i);
    }

    void reserve(int capacity) {
        data.reserve(capacity);
    }

    void swap(DArray<T>* t) {
        this->data.swap(t->data);
    }
};
}
#endif