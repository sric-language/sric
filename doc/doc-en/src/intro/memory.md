# How Memory Safety Works

Memory safety, performance, and simplicity form an impossible triangle - choosing any two necessitates sacrificing the third. Sric innovatively adopts runtime memory safety checks, eliminating overhead while significantly reducing memory vulnerabilities.

## Why Not Rust
Both Sric and Rust are memory-safe languages without garbage collection, but they differ fundamentally. Rust enforces safety at compile-time, whereas Sric implements runtime memory checks. Rust's safety mechanisms impose numerous coding restrictions, forcing developers to write complex code. Such complexity not only harms readability but often fails to achieve zero-cost abstraction. Sric's safety layer is transparent, requiring no extra effort to achieve memory safety.

## Optional Memory Safety
Although Sric's memory checks have minimal overhead, they are disabled by default in Release mode to achieve zero-cost abstractions and maximize performance. So the standard workflow is to debug the code in Debug mode, ensure there are no memory issues, and then compile it in Release for deployment.

In other words, Sric is not fully memory-safe, safety becomes optional based on project requirements. For projects prioritizing safety over performance, compilation macros can enable memory safety in Release mode.

## Lifetime Verification
Sric's safety checks cover: Array bounds checking, Null pointer checking, Wild pointer prevention, Dangling pointer checking. The ownership model inherently prevents memory leaks and double-free errors. The other checks are relatively straightforward, the core challenge of memory safety lies in detecting dangling pointers, which essentially involves verifying memory lifetimes.

In Sric, a non-ownership pointer is a "fat pointer," which includes the actual pointer and a verification code, among other details. The object's memory also contains an identical verification code. When a pointer is created, its verification code matches the object's. When memory is released, this code is set to 0. Each time the pointer is used, the verification codes of the pointer and the object are compared. If they differ, it indicates the object's memory has been freed. An error is reported immediately, preventing the issue from propagating, making it easy to locate the problem.

Although the principle is straightforward, several challenges must be addressed: handling derived pointers (pointers to the middle of memory rather than the start), managing memory arrays, distinguishing between heap and stack allocations, and dealing with non-cooperative objects (e.g., C++ classes with no space for verification codes). Fortunately, Sric has resolved most of these issues.

## vs. Address Sanitizer
AddressSanitizer (ASan) has several limitations:

- False negatives (e.g., can't detect reallocated memory use)
- High overhead (memory and performance)
- Platform dependence (primarily Clang/GCC, some ARM-specific)
- Requires recompiling dependencies

Sric works in language level not system level. Sric provides more comprehensive checks with lower overhead, and works on any platform that supports C++.
