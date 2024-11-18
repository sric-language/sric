#include "Logger.h"

#include <stdarg.h>
#include <stdio.h>
#include <string.h>

#ifdef __ANDROID__
#include <android/log.h>
#endif

using namespace sric;

Log *Log::instance = nullptr;

Log &Log::cur() {
  if (instance == nullptr) {
    instance = new Log();
  }
  return *instance;
}

Log::Log(): _level(LogLevel::Debug) {
    listeners.push_back(new LogListener());
}

Log::~Log() {
    for (LogListener* l : listeners) { delete l; }
    listeners.clear();
}

const char *cf_LogLevel_str[] = { "debug", "info", "warn", "error", "silent" };
#define BUF_SIZE 4096

#ifdef __ANDROID__
static int getAndroidLogLevel(const Log::LogLevel level) {
    switch (level) {
        case Log::Debug: {
            return ANDROID_LOG_DEBUG;
        }
        case Log::Info: {
            return ANDROID_LOG_INFO;
        }
        case Log::Warn: {
            return ANDROID_LOG_WARN;
        }
        case Log::Err: {
            return ANDROID_LOG_ERROR;
        }
        default: {
            return ANDROID_LOG_VERBOSE;
        }
    }
}
#endif

void Log::LogListener::onLog(Log::LogRec &rec) {
  char buf[BUF_SIZE] = {0};

  time_t now;
  time(&now);
  char *timeStr = ctime(&now);
  size_t len = strlen(timeStr);
  if (timeStr[len-1] == '\n') {
    timeStr[len-1] = 0;
  }

  snprintf(buf, BUF_SIZE, "%s[%s]%c(%s,%d) %s", timeStr
           , rec.tag, cf_LogLevel_str[(int)rec.level][0], rec.func, rec.line, rec.msg);
  
  print(rec, buf);
}

void Log::LogListener::print(LogRec &rec, const char *msg) {
  puts(msg);
  fflush(stdout);
#ifdef __ANDROID__
  int androidLevel = getAndroidLogLevel(rec.level);
  __android_log_print(androidLevel, rec.tag, "(%s,%d) %s\n", rec.func, rec.line, rec.msg);
#endif
}

void Log::doLog(const char *tag, const char *file, const char *func, const unsigned int line
                , const LogLevel level, const char *msg, ...) {
  va_list args;
  char buffer[BUF_SIZE];
  va_start(args, msg);

  if (level < this->getLevel()) {
    va_end(args);
    return;
  }

  vsnprintf(buffer, BUF_SIZE, msg, args);
    
  LogRec rec = { tag, file, func, line, level, buffer };

  lock.lock();
  for (LogListener* l : listeners) {
    if (level < l->level) continue;
    l->onLog(rec);
  }
  lock.unlock();

  va_end(args);
}

FileLogListener::FileLogListener(const char *path) {
  file = fopen(path, "w");
  if (!file) {
    printf("ERROR: open file error: %s\n", path);
  }
}

FileLogListener::~FileLogListener() {
  fclose(file);
  file = NULL;
}
void FileLogListener::print(Log::LogRec &rec, const char *msg) {
  //fprintf(file, msg);
  fputs(msg, file);
  fflush(file);
}