#include "reflection.h"
#include "sric.h"

#include <map>

std::map<std::string, sric::RModule>* g_reflectDb = nullptr;

void sric::registModule(sric::RModule* m) {
	if (g_reflectDb == nullptr) {
		g_reflectDb = new std::map<std::string, sric::RModule>();
	}
	(*g_reflectDb)[m->name] = *m;
}

sric::RefPtr<sric::RModule> sric::findModule(const char* name) {
	if (!g_reflectDb) {
		return nullptr;
	}
	auto it = g_reflectDb->find(name);
	if (it == g_reflectDb->end()) {
		return sric::RefPtr<RModule>();
	}
	sric::RModule* m = &it->second;
	return sric::RefPtr<RModule>(m);
}
