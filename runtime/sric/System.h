/*
 * Copyright (c) 2012-2016, chunquedong
 *
 * This file is part of cppfan project
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE version 3.0
 *
 * History:
 *   2012-12-23  Jed Young  Creation
 */

#ifndef _SRIC_SYSTEM_H_
#define _SRIC_SYSTEM_H_

#include <stdint.h>

namespace sric {

typedef int64_t NanosTime;
typedef int64_t MillisTime;


/*========================================================================
* Time
*/
const int64_t NanoPerSecond = 1000000000L;

/**
* return the milliseconds from midnight, January 1, 1970 UTC.
*/
MillisTime currentTimeMillis();

/**
* returns a relative time.
* Typically it is the number of nanosecond ticks which have elapsed since system startup.
*/
NanosTime nanoTicks();

inline MillisTime millisTicks() { return nanoTicks()/1000000L; }

/*========================================================================
* Thread
*/
/**
* sleep current thread millitm second
*/
void sleep(MillisTime millitm);

/**
* get current execable file path
*/
bool getSelfPath(char *selfname);

/**
* Thread ID
*/
uint64_t currentThreadId();


}
#endif /* TimeUtil_hpp */