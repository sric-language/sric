#include "sric/Refable.h"
#include "sric/common.h"
#include <thread>

namespace sric
{

CheckCodeType checkCodeCount = 0;

CheckCodeType generateCheckCode() {
    return (++checkCodeCount);
}

}
