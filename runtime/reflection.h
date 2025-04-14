
#ifndef SRIC_REFLECT_H_
#define SRIC_REFLECT_H_

#include "RefPtr.h"

namespace sric {
	struct RModule;
	struct RType;

	void registModule(RModule* m);
	RefPtr<RModule> findModule(const char* name);
	OwnPtr<void> newInstance(RType& type);

	inline const char* typeOf(void *obj) {
		if (!obj) return "";
		Reflectable* r = (Reflectable*)obj;
		return r->__typeof();
	}

	inline bool callPVFunc(void* func, void *arg) {
		if (!func) return false;
		void (*f)(void *a) = (void (*)(void *a))func;
		f(arg);
		return true;
	}

	inline void* callVPFunc(void* func) {
		if (!func) return nullptr;
		void* (*f)() = (void* (*)())func;
		return f();
	}
}

#define SC_AUTO_REGIST_MODULE(name) \
	class ScAutoRegistModule ## name { \
	public:\
		ScAutoRegistModule ## name () {\
			registReflection_ ## name();\
		}\
	};\
	ScAutoRegistModule ## name __scAutoRegistModuleInstance;

#endif