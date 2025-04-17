#include "sric/reflection.h"
#include "sric.h"

#include <map>

std::map<std::string, sric::StackRefable<sric::RModule> >* g_reflectDb = nullptr;

void sric::registModule(sric::RModule* m) {
	if (g_reflectDb == nullptr) {
		g_reflectDb = new std::map<std::string, sric::StackRefable<sric::RModule> >();
	}
	(*g_reflectDb)[m->name] = std::move(*m);
}

sric::RefPtr<sric::RModule> sric::findModule(const char* name) {
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

sric::OwnPtr<void> sric::newInstance(sric::RType& type) {
	sric::OwnPtr<void> (*func)() = (sric::OwnPtr<void> (*)())type.ctor;
	return func();
}