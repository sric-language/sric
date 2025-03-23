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
    String(const char* c, int size) : str(c, size) {}
    String(const std::string& c) : str(c) {}

    String(String&& other) {
        str = std::move(other.str);
    }

    String& operator=(String&& other) {
        str = std::move(other.str);
        return *this;
    }

    String copy() const {
        return String(this->str);
    }

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

    bool isEmpty() {
        return str.size() > 0;
    }

    int hashCode() const;
    int compare(const String& other) const;

    bool iequals(const String& other) const;
    bool contains(const String& s) const;
    bool startsWith(const String& s) const;
    bool endsWith(const String& s) const;
    int find(const String& s, int start = 0) const;

    String operator+(const String& other) {
        plus(other);
    }
    String plus(const String& other) const;
    void add(const char* cstr);

    void replace(const String& src, const String& dst);
    DArray<String> split(const String& sep) const;
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


    static String fromChar(uint32_t c);
    uint32_t getCharAt(int bytePos, int32_t* byteSize = nullptr) const;
    uint32_t getChar(int i) const;
    int charCount()const;
private:
    int charByteIndex(int i) const;
public:
};


inline String asStr(const char* cstr) {
    return String(cstr);
}

inline String strStatic(const char* cstr) {
    return String(cstr);
}

}
#endif