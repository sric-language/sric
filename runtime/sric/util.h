
#ifndef SRIC_UTIL_H_
#define SRIC_UTIL_H_

#include "sric/OwnPtr.h"
#include "sric/Refable.h"

#ifdef SC_CHECK
	#define sc_notNull(t) sric::notNull(t)
#else
	#define sc_notNull(t) (t)
#endif

namespace sric {


	template<typename T>
	inline T* unsafeAlloc() {
		return new T();
	}

	template<typename T>
	inline void unsafeFree(T* p) {
		delete p;
	}

	template<typename T>
	inline bool ptrIs(void* p) {
		return dynamic_cast<T*>(p) != nullptr;
	}

	template<typename T, typename F>
	inline bool ptrIs(F& p) {
		return dynamic_cast<T*>(p.get()) != nullptr;
	}

	template<typename T>
	inline T* notNull(T* p) {
		sc_assert(p != nullptr, "Non-Nullable");
		return p;
	}

	template<typename T>
	inline OwnPtr<T>&& notNull(OwnPtr<T>&& p) {
		sc_assert(!p.isNull(), "Non-Nullable");
		return std::move(p);
	}

	template<typename T>
	inline T& notNull(const T& p) {
		sc_assert(!p.isNull(), "Non-Nullable");
		return (T&)p;
	}

	template<typename T>
	inline std::function<T>& notNull(const std::function<T>& p) {
		sc_assert(p != nullptr, "Non-Nullable");
		return (std::function<T>&)p;
	}

	template<typename T>
	inline int hashCode(const T& p) {
		return p.hashCode();
	}

	template<typename T>
	inline int hashCode(const int p) {
		return p;
	}

	template<typename T>
	inline int compare(const T& a, const T& b) {
		return a.compare(b);
	}

	template<typename T>
	inline int compare(const int a, const int b) {
		return a - b;
	}


	template<typename T>
	inline OwnPtr<T> copy(const OwnPtr<T>& b) {
		return share(const_cast<OwnPtr<T>&>(b));
	}

	template<typename T>
	inline typename std::enable_if<!std::is_copy_constructible<T>::value, T>::type copy(const T& b) {
		return b.copy();
	}
	template<typename T>
	inline typename std::enable_if<std::is_copy_constructible<T>::value, T>::type copy(const T& b) {
		return b;
	}

	template<typename T>
	inline T unsafeCast(void* b) {
		return (T)b;
	}
}
//
//inline bool isNull(void* p) {
//	return p == nullptr;
//}

#endif