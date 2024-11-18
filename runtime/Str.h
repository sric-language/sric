#ifndef _SRIC_STRING_H_
#define _SRIC_STRING_H_

#include <string>
#include "DArray.h"

namespace sric
{

class String : public Noncopyable {
    std::string str;
public:
    String() {};
    String(const char* c) : str(c) {}
    String(const std::string& c) : str(c) {}

    const std::string& cpp_str() const {
        return str;
    }
    const char* c_str() const {
        return str.c_str();
    }
    void resize(int size) {
        str.resize(size);
    }
    char* data() {
        return (char*)str.data();
    }
    int size() const {
        return str.size();
    }
    char get(int i) const {
        return str[i];
    }
    char operator[](int i) const {
        return get(i);
    }

    int hashCode() const;
    int compare(const RefPtr<String> other) const;

    bool iequals(RefPtr<String> other) const;
    bool contains(RefPtr<String> s) const;
    bool startsWith(RefPtr<String> s) const;
    bool endsWith(RefPtr<String> s) const;
    int find(RefPtr<String> s, int start = 0) const;

    String& operator+(RefPtr<String> other) {
        plus(other);
    }
    String& plus(RefPtr<String> other);
    void add(const char* cstr);

    void replace(RefPtr<String> src, RefPtr<String> dst);
    DArray<String> split(RefPtr<String> sep) const;
    String substr(int pos, int len = -1) const;

    void trimEnd();
    void trimStart();
    void trim() { trimStart(); trimEnd(); }
    void removeLastChar();

    String toLower() const;
    String toUpper() const;

    int toInt32() const;
    int64_t toInt64() const;
    float toFloat32() const;
    double toFloat64() const;

    static String fromInt32(int i);
    static String fromInt64(int64_t i);
    static String fromFloat64(double f);
    static String fromFloat32(float f);

    /**
    * 'printf' style format
    */
    static String format(const char* fmt, ...);
};


inline String asStr(const char* cstr) {
    return String(cstr);
}

inline String strStatic(const char* cstr) {
    return String(cstr);
}

}
#endif