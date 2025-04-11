
#ifndef SRIC_UTIL_H_
#define SRIC_UTIL_H_

#include "Ptr.h"
#include "Refable.h"

namespace sric {


	template<typename T>
	T* unsafeAlloc() {
		return new T();
	}

	template<typename T>
	void unsafeFree(T* p) {
		delete p;
	}

	template<typename T>
	bool ptrIs(void* p) {
		return dynamic_cast<T*>(p) != nullptr;
	}

	template<typename T, typename F>
	bool ptrIs(F& p) {
		return dynamic_cast<T*>(p.get()) != nullptr;
	}

	template<typename T>
	T* notNull(T* p) {
		sc_assert(p != nullptr, "Non-Nullable");
		return p;
	}

	template<typename T>
	OwnPtr<T> notNull(OwnPtr<T>&& p) {
		sc_assert(!p.isNull(), "Non-Nullable");
		return p;
	}

	template<typename T>
	T& notNull(const T& p) {
		sc_assert(!p.isNull(), "Non-Nullable");
		return (T&)p;
	}

	template<typename T>
	std::function<T>& notNull(const std::function<T>& p) {
		sc_assert(p != nullptr, "Non-Nullable");
		return (std::function<T>&)p;
	}

	template<typename T>
	int hashCode(const T& p) {
		return p.hashCode();
	}

	template<typename T>
	int hashCode(const int p) {
		return p;
	}

	template<typename T>
	int compare(const T& a, const T& b) {
		return a.compare(b);
	}

	template<typename T>
	int compare(const int a, const int b) {
		return a - b;
	}


	template<typename T>
	OwnPtr<T> copy(const OwnPtr<T>& b) {
		return share(const_cast<OwnPtr<T>&>(b));
	}

	template<typename T>
	typename std::enable_if<!std::is_copy_constructible<T>::value, T>::type copy(const T& b) {
		return b.copy();
	}
	template<typename T>
	typename std::enable_if<std::is_copy_constructible<T>::value, T>::type copy(const T& b) {
		return b;
	}

	inline const char* typeOf(void *obj) {
		if (!obj) return "";
		Reflectable* r = (Reflectable*)obj;
		return r->__typeof();
	}

	template<typename T>
	T unsafeCast(void* b) {
		return (T)b;
	}
}
//
//inline bool isNull(void* p) {
//	return p == nullptr;
//}

#endif