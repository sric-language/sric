
#ifndef SRIC_REFLECT_H_
#define SRIC_REFLECT_H_

#include "sric/OwnPtr.h"
#include "sric/Str.h"

namespace sric {
	struct RModule;
	struct RType;

	void registModule(RModule* m);
	RefPtr<RModule> findModule(const char* name);
	OwnPtr<void> newInstance(RType& type);

	const char* typeOf(void *obj);

	bool callPtrToVoid(void* func, void *arg);

	void* callVoidToPtr(void* func);

	String callInstanceToString(void* func, void *instance);

	bool callInstanceStringToBool(void* func, void *instance, String s);

}

#define SC_AUTO_REGIST_MODULE(name) \
	class ScAutoRegistModule ## name { \
	public:\
		ScAutoRegistModule ## name () {\
			registReflection_ ## name();\
		}\
	};\
	ScAutoRegistModule ## name _scAutoRegistModuleInstance_ ## name;

#endif