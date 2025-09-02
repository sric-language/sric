#ifndef _SRIC_COMMON_H_
#define _SRIC_COMMON_H_

#include <stdlib.h>
#include <stdio.h>

#if !defined(SC_NO_CHECK) && !defined(SC_CHECK) && !defined(SC_NO_AUTO_DEFINE)

#ifdef _DEBUG
    #define SC_CHECK
#endif

#ifdef NDEBUG
    #define SC_NO_CHECK
#endif

#endif

#define SC_NOTHROW noexcept
//#define SC_NOTHROW 

#define SC_OFFSETOF(type, member) ((size_t)&(((type*)0)->member))

#if __cplusplus >= 201703L
    #define SC_UNUSED [[maybe_unused]]
#elif defined(__GNUC__)
    #define SC_UNUSED __attribute__((unused))
#else
    #define SC_UNUSED
#endif

namespace sric
{

#ifndef SC_CHECK
    #define sc_assert(c, m) 
#else
    #define sc_assert(c, msg) \
        if (!(c)) {\
            fprintf(stderr, "ERROR: %s\n", msg);\
            abort();\
        }
#endif // SC_CHECK

    inline void verify(bool c, const char* msg = nullptr) {
        if (!(c)) {
            if (msg) fprintf(stderr, "verify fail: %s\n", msg);
            abort();
        }
    }

    class Noncopyable {
    public:
        Noncopyable() = default;
    protected:
        Noncopyable(const Noncopyable&) = delete;
        Noncopyable& operator=(const Noncopyable&) = delete;
    };

    class Reflectable {
    public:
        virtual const char* _typeof() const SC_NOTHROW = 0;
    };
}

#endif