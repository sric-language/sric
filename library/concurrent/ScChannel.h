
#ifndef _CONCURRENT_CHANNEL_H_
#define _CONCURRENT_CHANNEL_H_

#include <mutex>
#include <condition_variable>
#include <list>

namespace concurrent {

	template<typename T>
	struct Channel {
		std::list<T> _queue;
		std::condition_variable _condVar;
		std::mutex _mutex;
		bool _canceled;

		T read() {
			std::unique_lock<std::mutex> lk(_mutex);
			while (!_canceled) {
				if (_queue.size() > 0) {
					T t = std::move(_queue.front());
					_queue.pop_front();
					return t;
				}
				_condVar.wait(lk);
			}
			return T();
		}
		void write(T t) {
			{
				std::lock_guard<std::mutex> lock(_mutex);
				_queue.push_back(t);
			}
			_condVar.notify_one();
		}

		void clear() {
			std::lock_guard<std::mutex> lock(_mutex);
			_queue.clear();
		}

		void cancel() {
			std::lock_guard<std::mutex> lock(_mutex);
			_canceled = true;
			_condVar.notify_all();
		}

		bool isCanceled() {
			return _canceled;
		}
	};

}
#endif //_CONCURRENT_CHANNEL_H_
