#ifndef _SRIC_COMMON_H_
#define _SRIC_COMMON_H_

#include <stdlib.h>
#include <stdio.h>

#if !defined(SC_NO_CHECK) && !defined(SC_CHECK)

#ifdef _DEBUG
    #define SC_CHECK
#endif

#ifdef NDEBUG
    #define SC_NO_CHECK
#endif

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
        virtual const char* __typeof() const = 0;
    };
}

#endif