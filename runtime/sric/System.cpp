/*
 * Copyright (c) 2012-2016, chunquedong
 *
 * This file is part of cppfan project
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE version 3.0
 *
 * History:
 *   2012-12-23  Jed Young  Creation
 */
#include "sric/System.h"

#include <stdio.h>
#include <chrono>

int64_t sric::currentTimeMillis() {
    return std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();
}

int64_t sric::nanoTicks() {
    return std::chrono::duration_cast<std::chrono::nanoseconds>(std::chrono::steady_clock::now().time_since_epoch()).count();
}

//========================================================================
#ifndef _WIN32

  #include <sys/types.h>
  #include <time.h>
  #include <unistd.h>

  void sric::sleep(MillisTime millitm) {
    usleep(millitm * 1000);
  }

  bool sric::getSelfPath(char *selfname) {
    const char *sysfile = "/proc/self/exe";
    int  namelen = 256;
    //memset(selfname, 0, 256);
    if ( -1 != readlink( sysfile, selfname, namelen) ) {
      return true;
    }
    return false;
  }

#else
/*
 * Windows
 */

#include <Windows.h>

  void sric::sleep(MillisTime millitm) {
    Sleep(millitm);
  }

  bool sric::getSelfPath(char *selfname) {
    //TCHAR szFileName[MAX_PATH];
    GetModuleFileNameA(NULL,selfname,MAX_PATH);
    return true;
  }

#endif //CF_WIN

//========================================================================
#include <thread>

uint64_t sric::currentThreadId() {
#ifdef _WIN32
  //unsigned long GetCurrentThreadId(void);
  return GetCurrentThreadId();
#else
  //unsigned long pthread_self(void);
  return (uint64_t)pthread_self();
#endif
}
