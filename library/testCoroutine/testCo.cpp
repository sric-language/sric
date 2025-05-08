
#include <thread>
#include "sric.h"
#include "CoScheduler.h"

namespace testCoroutine {

void installCoScheduler() {
    sric::call_later = [](std::function<void()> h){
        CoScheduler::cur()->call_later(h);
    };
}

sric::Promise<int> testCallback() {
    auto resultData = std::make_shared<sric::Promise<int>::ResultData >();
    std::thread([=]() {
        std::this_thread::sleep_for(std::chrono::seconds(1));
        resultData->on_done(1);
    }).detach();

    return sric::Promise<int>(resultData);
}

int runCoroutine() {
    return CoScheduler::run();
}

}