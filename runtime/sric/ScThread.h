
#ifndef _CONCURRENT_THREAD_H_
#define _CONCURRENT_THREAD_H_

#include "sc_runtime.h"

#if !defined(__EMSCRIPTEN__) || defined(__EMSCRIPTEN_PTHREADS__)
	#include <thread>
#endif

#include <mutex>
#include <condition_variable>

#include "ScChannel.h"

namespace sric {

	struct Mutex {
		std::recursive_mutex _mutex;
		void lock() { _mutex.lock(); }
		bool tryLock() { return _mutex.try_lock(); }
		void unlock() { _mutex.unlock(); }
	};

	template<typename T>
	struct LockGuard {
		T& _t;
		LockGuard(T& t) : _t(t) { _t.lock(); }
		~LockGuard() { _t.unlock(); }
	};

	struct CondVar {
		std::condition_variable_any _condVar;
		void notifyOne() { _condVar.notify_one(); }
		void notifyAll() { _condVar.notify_all(); }
		void wait(Mutex& mutex) { _condVar.wait(mutex._mutex); }
		void waitFor(Mutex& mutex, int64_t timeout) {
			_condVar.wait_for(mutex._mutex, std::chrono::milliseconds(timeout));
		}
	};

#if !defined(__EMSCRIPTEN__) || defined(__EMSCRIPTEN_PTHREADS__)
	struct Thread {
		std::thread _thread;

		Thread() {}
		Thread(Thread&& t): _thread(std::move(t._thread)) {
		}

		void join() { _thread.join(); }
		void detach() { _thread.detach(); }
		~Thread() {
			if (_thread.joinable()) {
				_thread.detach();
			}
		}

		template<typename T>
		static Thread make(void (*ThreadFunc)(sric::OwnPtr<Channel<T> >), sric::OwnPtr < Channel<T> > args) {
			sric::AutoMove<sric::OwnPtr < Channel<T> > > argsWrap(args);
			
			Thread thd;
			thd._thread = std::thread([=]() mutable {
				ThreadFunc(argsWrap.take());
			});
			return std::move(thd);
		}
	};
#endif
}
#endif //_CONCURRENT_THREAD_H_
