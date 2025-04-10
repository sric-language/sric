
#ifndef SRIC_REFLECT_H_
#define SRIC_REFLECT_H_

#include "RefPtr.h"

namespace sric {
	struct RModule;
	struct RType;
	void registModule(RModule* m);
	RefPtr<RModule> findModule(const char* name);
	OwnPtr<void> newInstance(RType& type);
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