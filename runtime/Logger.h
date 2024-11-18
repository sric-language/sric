#ifndef _SC_LOG_H_
#define _SC_LOG_H_

#include <stdarg.h>
#include <vector>
#include <mutex>

namespace sric {
enum struct LogLevel {
    Debug, Info, Warn, Err, Silent
};

class Log {
public:
    struct LogRec {
        const char* tag;
        const char* file;
        const char* func;
        const unsigned int line;
        const LogLevel level;
        const char* msg;
    };

    struct LogListener {
        LogLevel level = LogLevel::Debug;
        virtual void onLog(LogRec& rec);
        virtual void print(LogRec& rec, const char* msg);
        virtual ~LogListener() {}
    };

    std::vector<LogListener*> listeners;
private:
    LogLevel _level;
public:
    LogLevel getLevel() { return _level; }
    void setLevel(LogLevel l) { _level = l; }
private:
    std::mutex lock;
    static Log* instance;
public:
    static Log& cur();

    Log();
    ~Log();

    bool isDebug() {
        return isEnableLevel(LogLevel::Debug);
    }

    bool isEnableLevel(LogLevel alevel) {
        return alevel >= getLevel();
    }

    void doLog(const char* tag, const char* file, const char* func, const unsigned int line
        , const LogLevel level, const char* msg, ...);
};

class FileLogListener : public Log::LogListener {
    FILE* file;
public:
    FileLogListener(const char* path);
    ~FileLogListener();
    virtual void print(Log::LogRec& rec, const char* msg);
};

}

/*========================================================================
 * sys logging macro
 */

#define sc_Log_log(tag, level, msg, ...) do {\
  if (Log::cur().isEnableLevel(level))\
    Log::cur().doLog(tag, __FILE__, __func__, __LINE__,  level, msg, ## __VA_ARGS__);\
} while(false)

/**
 * convenience log macro.
 *
 */
#define scError(tag, msg, ...) sc_Log_log(tag, Log::Err, msg, ## __VA_ARGS__)
#define scWarn(tag, msg, ...)  sc_Log_log(tag, Log::Warn, msg, ## __VA_ARGS__)
#define scInfo(tag, msg, ...)  sc_Log_log(tag, Log::Info, msg, ## __VA_ARGS__)

#ifdef _DEBUG
  #define scDebug(tag, msg, ...) sc_Log_log(tag, Log::Debug, msg, ## __VA_ARGS__)
#else
  #define scDebug(tag, msg, ...)
#endif

#endif