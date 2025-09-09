
## 协程

Sric的协程和Javascript的几乎完全一致，下面是协程的例子：
```
async fun test2() : Int {
    var i = 0
    i = await testCallback()
    printf("await result:%d\n", i)
    return i + 1
}
```

- 协程函数使用async标记
- await的目标必须是async函数，或者返回值为`Promise$<T>`的函数。
- async函数的返回值会被编译器自动包装成`Promise$<T>`

## 协程C++适配

#### 接入主循环

Sric协程通过主循环来调度。UI框架的主线程有主循环，服务端库libevent、libev、libuv、libhv都有主循环。他们的主循环写法都不太一样，伪代码如下：

```
sric::call_later = [](std::function<void()> h){
    call_in_loop([]{
        h();
    });
};
```

#### 适配异步回调接口

堆上分配sric::Promise<T>::ResultData对象，在回调中调用其on_done方法。
```
sric::Promise<int> testCallback() {
    auto resultData = std::make_shared<sric::Promise<int>::ResultData >();
    std::thread([=]() {
        std::this_thread::sleep_for(std::chrono::seconds(1));
        resultData->on_done(1);
    }).detach();
    return sric::Promise<int>(resultData);
}
```
