//
// Copyright (c) 2025, chunquedong
// Licensed under the Academic Free License version 3.0
//


#ifndef _SRIC_PROMISE_H_
#define _SRIC_PROMISE_H_

#if defined(__cpp_coroutines) || __cplusplus >= 202002L || _MSVC_LANG >= 202002L
    #define HAS_COROUTINES 1
#endif

#if HAS_COROUTINES

#include <coroutine>
#include <functional>
#include <memory>

namespace sric
{

typedef std::function<void()> Resume;
extern std::function<void(Resume)> call_later;

template<typename T>
struct Promise {
    struct ResultData {
        std::coroutine_handle<> continuation;
        bool is_ready = false;
        int error = 0;
        T value{};

        std::function<void(int, T)> then;

        void on_done(T r) {
            value = r;
            is_ready = true;
            call_later([=]{
                if (this->then) {
                    this->then(this->error, this->value);
                }
                this->continuation.resume();
            });
        }
    };

    struct promise_type {
        std::shared_ptr<ResultData> result = std::make_shared<ResultData>();

        std::suspend_never initial_suspend() { return {}; }
        std::suspend_never final_suspend() noexcept { return {}; }
        void unhandled_exception() {
            result->error = 1;
            result->is_ready = true;
        }
        Promise get_return_object() {
            return Promise(result);
        }
        //void return_void() {}
        void return_value(T value) {
            result->value = value;
            result->is_ready = true;
            
            if (result->continuation) {
                result->continuation.resume();
            }
        }
        auto yield_value(T v)
        {
            result->value = v;
            result->is_ready = true;
            return std::suspend_always{};
        }
    };

    std::shared_ptr<ResultData> result;

    Promise(std::shared_ptr<ResultData>& res) {
        result = res;
    }

    void then(std::function<void(int, T)> cb) {
        result->then = cb;
    }

    int get_error() {
        if (result->is_ready) {
            return result->error;
        }
        return 0;
    }
    T get_result() {
        if (result->is_ready) {
            return result->value;
        }
        return T{};
    }
    bool is_done() {
        return result->is_ready;
    }

    bool await_ready() {
        return is_done();
    }
    void await_suspend(std::coroutine_handle<> h) {
        result->continuation = h;
    }
    T await_resume() {
        return get_result();
    }
};

template<>
struct Promise<void> {
    struct promise_type {
        int error = 0;
        std::coroutine_handle<> continuation;
        std::function<void(int)> then;

        std::suspend_never initial_suspend() { return {}; }
        std::suspend_never final_suspend() noexcept { return {}; }
        void unhandled_exception() {
            error = 1;
        }
        Promise get_return_object() {
            return Promise(std::coroutine_handle<promise_type>::from_promise(*this));
        }
        void return_void() {
            if (this->then) {
                this->then(this->error);
            }
            if (this->continuation) {
                this->continuation.resume();
            }
        }
    };
private:
    std::coroutine_handle<promise_type> handle;
public:
    Promise(std::coroutine_handle<promise_type> h)
        : handle(h) {
    }
    Promise(const Promise&) = delete;
    Promise& operator=(const Promise&) = delete;

    void then(std::function<void(int)> cb) {
        handle.promise().then = cb;
    }

    bool await_ready() { return false; }
    void await_suspend(std::coroutine_handle<> h) {
        handle.promise().continuation = h;
    }
    void await_resume() {}
};

}

#endif //HAS_COROUTINES
#endif //_SRIC_PROMISE_H_
