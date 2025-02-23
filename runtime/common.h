#ifndef _SRIC_COMMON_H_
#define _SRIC_COMMON_H_

#ifndef _DEBUG
    #define SC_NO_CHECK
#elif defined(NDEBUG)
    #define SC_NO_CHECK
#endif

namespace sric
{

#ifdef SC_NO_CHECK
    #define sc_assert(c, m) 
#else
    #define sc_assert(c, msg) \
        if (!(c)) {\
            printf("ERROR: %s\n", msg);\
            abort();\
        }
#endif // SC_NO_CHECK


    class Noncopyable {
    public:
        Noncopyable() = default;
    protected:
        Noncopyable(const Noncopyable&) = delete;
        Noncopyable& operator=(const Noncopyable&) = delete;
    };

}

#endif