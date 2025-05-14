#include "sric/Refable.h"
#include "sric/common.h"
#include <thread>

namespace sric
{

uint32_t checkCodeCount = 0;

uint32_t generateCheckCode() {
    std::hash<std::thread::id> hasher;
    size_t tid = hasher(std::this_thread::get_id());
    return (tid << 24) | (++checkCodeCount);
}

}
