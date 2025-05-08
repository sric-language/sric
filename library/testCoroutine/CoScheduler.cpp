#include "CoScheduler.h"

#if HAS_COROUTINES

using namespace testCoroutine;

CoScheduler* CoScheduler::instance = new CoScheduler();

void CoScheduler::call_later(std::function<void()> h) {
    _lock.lock();
    _task.push_back(h);
    _lock.unlock();
}

int CoScheduler::run() {
    int c = 0;
    CoScheduler* self = CoScheduler::cur();
    while (true) {
        std::function<void()> handle;
        self->_lock.lock();
        if (self->_task.size() > 0) {
            handle = self->_task.front();
            self->_task.pop_front();
        }
        self->_lock.unlock();

        if (handle) {
            handle();
            ++c;
        }
        else {
            break;
        }
    }
    return c;
}


#endif //HAS_COROUTINES