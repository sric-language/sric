#ifndef SRIC_FILESYSTEM_H_
#define SRIC_FILESYSTEM_H_

#include "sric/Stream.h"
#include "sric/Ptr.h"

namespace sric
{

/**
 * Defines a set of functions for interacting with the device file system.
 */
struct FileSystem
{
    static bool mkdirs(const char* path);
    static bool listFiles(const char* dirPath, DArray<String>& files);
    static bool exists(const char* filePath);
    static bool isDir(const char* filePath);
    static int64_t fileSize(const char* filePath);
    static uint64_t modifiedTime(const char* filePath);
    static bool moveTo(const char* from, const char* to);

    static bool copyTo(const char* from, const char* to);
    static bool remove(const char* filePath);


    static bool isAbsolute(const char* filePath);
    static String canonicalPath(const char* filePath);
    static String fileName(const char* path);
    static String getExtName(const char* path);
    static String getBaseName(const char* path);
    static String getParentPath(const char* path);

};

}

#endif
