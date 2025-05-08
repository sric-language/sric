
#ifndef _SRIC_COSCHEDULER_H_
#define _SRIC_COSCHEDULER_H_

#if defined(__cpp_coroutines) || __cplusplus >= 202002L || _MSVC_LANG >= 202002L
    #define HAS_COROUTINES 1
#endif

#if HAS_COROUTINES

#include <coroutine>
#include <mutex>
#include <list>
#include <functional>

namespace testCoroutine
{

struct CoScheduler {
protected:
    std::mutex _lock;
    std::list<std::function<void()> > _task;
public:
    static CoScheduler* instance;
    static CoScheduler* cur() {
        return instance;
    }

    virtual void call_later(std::function<void()> h);

    static int run();
};


}

#endif //HAS_COROUTINES

#endif //_SRIC_COSCHEDULER_H_
