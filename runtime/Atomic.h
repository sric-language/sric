#ifndef _SC_ATOMIC_H_
#define _SC_ATOMIC_H_

#include <atomic>

template<typename T>
struct Atomic {
	std::atomic<T> data;

	T get() {
		return data;
	}

	void set(T t) {
		data = t;
	}

	bool compareAndSwap(T expected, T desired) {
        return data.compare_exchange_strong(expected, desired);
	}
};

#endif