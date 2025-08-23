
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
		bool _canceled = false;

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
		int size() {
			std::lock_guard<std::mutex> lock(_mutex);
			return _queue.size();
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

	template<typename T>
	class ThreadPool {
		Channel<T> _channel;
		std::vector<std::thread> threadList;
		int threadSize;
	public:
		ThreadPool(int athreadSize = 4) : threadSize(athreadSize) {
		}
		~ThreadPool() {
			stop();
		}

		void enterPoint() {
			while (true) {
				auto msg = _channel.read();
				if (!msg->isCanceled()) {
					msg->run();
				}
			}
		}

		void addTask(T t) {
			_channel.write(t);
		}


		void start() {
			if (threadList.size() == threadSize) {
				return;
			}

			for (int i = 0; i < threadSize; ++i)
			{
				threadList.push_back(std::thread(&ThreadPool::enterPoint, this));
			}
		}

		void stop() {
			_channel.clear();
			for (int i = 0; i < threadList.size(); ++i) {
				threadList[i].join();
			}
			threadList.clear();
		}
	};
}
#endif //_CONCURRENT_CHANNEL_H_
