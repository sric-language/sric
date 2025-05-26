
#ifndef SRIC_OPTINAL_H_
#define SRIC_OPTINAL_H_

#include "sric/common.h"

namespace sric {
	template<typename T>
	struct Optional {
    private:
		char storage_[sizeof(T)];
        int error_;
    public:
        Optional() noexcept : error_(1) {}

        Optional(const T& value) : error_(0) {
            new (&storage_) T(value);
        }

        Optional(T&& value) : error_(0) {
            new (&storage_) T(std::move(value));
        }

        Optional(const Optional& other) : error_(other.error_) {
            if (!other.error_) {
                new (&storage_) T(*other);
            }
        }

        Optional(Optional&& other) noexcept : error_(other.error_) {
            if (!other.error_) {
                new (&storage_) T(std::move(*other));
                other.error_ = 1;
            }
        }

        ~Optional() {
            reset();
        }

        Optional& operator=(const Optional& other) {
            if (this != &other) {
                reset();
                error_ = other.error_;
                if (!other.error_) {
                    new (&storage_) T(*other);
                }
            }
            return *this;
        }

        Optional& operator=(Optional&& other) noexcept {
            if (this != &other) {
                reset();
                error_ = other.error_;
                if (!other.error_) {
                    new (&storage_) T(std::move(*other));
                    other.error_ = 1;
                }
            }
            return *this;
        }

        bool hasValue() const noexcept {
            return error_;
        }

        int getError() {
            return error_;
        }

        void setValue(const T& t) {
            reset();
            new (&storage_) T(t);
            error_ = 0;
        }

        void setValue(T&& t) {
            reset();
            new (&storage_) T(std::move(t));
            error_ = 0;
        }

        void setError(int e = 1) {
            reset();
            error_ = e;
        }

        T& value() & {
            sc_assert(!error_, "Bad optional access");
            return *reinterpret_cast<T*>(&storage_);
        }

        const T& value() const & {
            sc_assert(!error_, "Bad optional access");
            return *reinterpret_cast<const T*>(&storage_);
        }

        T&& value() && {
            sc_assert(!error_, "Bad optional access");
            return std::move(*reinterpret_cast<T*>(&storage_));
        }

        const T&& value() const && {
            sc_assert(!error_, "Bad optional access");
            return std::move(*reinterpret_cast<const T*>(&storage_));
        }

        T& operator*() & {
            return value();
        }

        const T& operator*() const & {
            return value();
        }

        T&& operator*() && {
            return std::move(value());
        }

        const T&& operator*() const && {
            return std::move(value());
        }

        T* operator->() {
            return &value();
        }

        const T* operator->() const {
            return &value();
        }

        template <typename U>
        T valueOr(U&& default_value) const {
            return (!error_) ? value() : static_cast<T>(std::forward<U>(default_value));
        }

        inline void reset() noexcept {
            if (!error_) {
                reinterpret_cast<T*>(&storage_)->~T();
                error_ = 1;
            }
        }
	};
}


#endif