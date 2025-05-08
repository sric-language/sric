#include "sric/Promise.h"

#if HAS_COROUTINES

namespace sric
{

std::function<void(Resume)> call_later;

}

#endif