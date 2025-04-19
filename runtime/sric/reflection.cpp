#include "sric/reflection.h"
#include "sric.h"

#include <map>

namespace sric {

std::map<std::string, sric::StackRefable<sric::RModule> >* g_reflectDb = nullptr;

void registModule(sric::RModule* m) {
	if (g_reflectDb == nullptr) {
		g_reflectDb = new std::map<std::string, sric::StackRefable<sric::RModule> >();
	}
	(*g_reflectDb)[m->name] = std::move(*m);
}

sric::RefPtr<sric::RModule> findModule(const char* name) {
	if (!g_reflectDb) {
		return sric::RefPtr<sric::RModule>();
	}
	auto it = g_reflectDb->find(name);
	if (it == g_reflectDb->end()) {
		return sric::RefPtr<RModule>();
	}
	sric::StackRefable<sric::RModule>& m = it->second;
	return sric::RefPtr<sric::RModule>(m);
}

sric::OwnPtr<void> newInstance(sric::RType& type) {
	sric::OwnPtr<void> (*func)() = (sric::OwnPtr<void> (*)())type.ctor;
	return func();
}

const char* typeOf(void *obj) {
	if (!obj) return "";
	Reflectable* r = (Reflectable*)obj;
	return r->_typeof();
}

bool callPtrToVoid(void* func, void *arg) {
	if (!func) return false;
	void (*f)(void *a) = (void (*)(void *a))func;
	f(arg);
	return true;
}

void* callVoidToPtr(void* func) {
	if (!func) return nullptr;
	void* (*f)() = (void* (*)())func;
	return f();
}


String callInstanceToString(void* func, void *instance) {
	if (!func) return "";
	auto f = (String (*)(void *ins))func;
	return f(instance);
}

bool callInstanceStringToBool(void* func, void *instance, String s) {
	if (!func) return false;
	auto f = (bool (*)(void *ins, String s))func;
	return f(instance, std::move(s));
}

}//ns