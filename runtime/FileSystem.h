#ifndef SRIC_FILESYSTEM_H_
#define SRIC_FILESYSTEM_H_

#include "Stream.h"
#include "Ptr.h"

namespace sric
{

/**
 * Defines a set of functions for interacting with the device file system.
 */
namespace FileSystem
{
    bool mkdirs(const char* path);
    bool listFiles(const char* dirPath, RefPtr<DArray<String>> files);
    bool exists(const char* filePath);
    bool isDir(const char* filePath);
    int64_t fileSize(const char* filePath);
    uint64_t modifiedTime(const char* filePath);
    bool moveTo(const char* from, const char* to);

    bool copyTo(const char* from, const char* to);
    bool remove(const char* filePath);


    bool isAbsolutePath(const char* filePath);
    String getDirName(const char* path);
    String getExtName(const char* path);
    String getBaseName(const char* path);
    String getParentPath(const char* path);

};

}

#endif
