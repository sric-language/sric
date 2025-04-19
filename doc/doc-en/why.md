## Why Sric
Sric is a high-performance programming language with memory safety.
The design of Sric draws heavily from C++, but it makes two key improvements:
  1. Reducing features and complexity.
  2. Adding memory safety.

### Design Philosophy
#### Performance
The notion that "modern computers are fast enough" is a childhood myth. Hardware advancements can’t keep up with the growing complexity of problems. Every industry I’ve encountered faces performance issues. To solve performance problems, the first step is to eliminate garbage collection (GC). Languages with GC inherently have an invisible performance ceiling. A high-performance language must provide low-level memory manipulation and flexible stack allocation. From the outset, Sric has been designed to match the performance of C/C++.

#### Memory Safety
Memory safety and performance are not mutually exclusive. However, Rust’s approach restricts code functionality, forces users to adapt their programming style, and increases the learning curve. Sric takes a different path—it introduces runtime memory safety checks, trading a slight performance overhead for safe. These checks are only enabled in debug mode. In release mode, Sric runs as fast as hand-written C++ code.

#### Abstraction Capabilities
Object-oriented programming (OOP) is a key measure of a language’s abstraction power. Although misuse of inheritance has given OOP a bad reputation, I believe it remains useful in certain scenarios. Sric supports OOP but imposes language-level restrictions on inheritance.

#### Simplicity and Ease
Both C++ and Rust have gone to the opposite extreme, attempting to cover every use case with complex features. Sric, on the other hand, strives to minimize features and avoid complexity. For example:

No multiple constructors, function overloading, or template metaprogramming (unlike C++).

No macros, intricate module systems, or lifetime annotations (unlike Rust).

Sric avoids excessive syntactic sugar. While syntactic sugar can reduce verbosity, too much of it increases learning costs and may harm readability. The goal is to strike a balance between ease of use and cognitive overhead.

#### Interoperability with C++
Sric seamlessly interoperates with C++ and can generate human-readable C++ code. C++ and C have long histories and vast ecosystems of high-quality libraries. Sric integrates smoothly into the C++ ecosystem, making it easy to leverage legacy code or call operating system APIs.

Additionally, Sric code can be easily invoked from C++, facilitating collaboration with developers who haven’t adopted Sric. For example, a team could use Sric internally while exposing C++ interfaces externally.
