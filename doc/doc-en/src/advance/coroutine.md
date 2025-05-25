## Coroutines

Sric's coroutines are almost identical to JavaScript's. Here's an example:

```sric
async fun test2() : Int {
    var i = 0;
    i = await testCallback();
    printf("await result:%d\n", i);
    return i + 1;
}
```
- Coroutine functions are marked with `async`
- The target of await must be either an async function or a function returning `Promise$<T>`
- The return value of async functions is automatically wrapped into `Promise$<T>` by the compiler

### C++ Coroutine Adaptation
#### Integrating with Event Loop
Sric coroutines are scheduled through the main event loop. The pseudocode shows integration with different event loop implementations:

```sric
sric::call_later = [](std::function<void()> h){
    call_in_loop([]{ 
        h();
    });
};
```
#### Adapting Asynchronous Callback Interfaces
Allocate `sric::Promise<T>::ResultData` on heap and call its on_done method in callback:

```sric
sric::Promise<int> testCallback() {
    auto resultData = std::make_shared<sric::Promise<int>::ResultData >();
    std::thread([=]() {
        std::this_thread::sleep_for(std::chrono::seconds(1));
        resultData->on_done(1);
    }).detach();
    return sric::Promise<int>(resultData);
}
```