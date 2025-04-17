
#include <sys/types.h>
#include <sys/stat.h>

#include <fcntl.h>
#include <sys/types.h>
#include <sys/stat.h>

#ifdef _WIN32
    #include <windows.h>
    #include <tchar.h>
    #include <stdio.h>
    #include <direct.h>
    #define gp_stat _stat
    #define gp_stat_struct struct stat
#else
    #define __EXT_POSIX2
    #include <libgen.h>
    #include <dirent.h>
    #define gp_stat stat
    #define gp_stat_struct struct stat
#endif

#include <filesystem>
#include <chrono>
#include <fstream>

#include "sric/FileSystem.h"
#include "sric/Stream.h"
#include "sric/FileStream.h"

#include <algorithm>
#include <assert.h>
#include <mutex>

namespace sric
{
static std::mutex __fileLock;

namespace fs = std::filesystem;

bool FileSystem::mkdirs(const char* cpath) {
    return fs::create_directories(cpath);
}

bool FileSystem::listFiles(const char* dirPath, DArray<String>& files)
{
    fs::path dir = fs::u8path(dirPath);
    for (auto& p : fs::directory_iterator(dir)) {
        String pathstr = p.path().string();
        files.add(std::move(pathstr));
    }
    return fs::is_directory(dir);
}

bool FileSystem::isDir(const char* filePath)
{
    return fs::is_directory(fs::u8path(filePath));
}

bool FileSystem::exists(const char* filePath)
{
    return fs::exists(fs::u8path(filePath));
}

int64_t FileSystem::fileSize(const char* filePath) {
    return fs::file_size(fs::u8path(filePath));
}

uint64_t FileSystem::modifiedTime(const char* filePath) {
    auto ftime = fs::last_write_time(fs::u8path(filePath));
    uint64_t mills = std::chrono::time_point_cast<std::chrono::milliseconds>(ftime).time_since_epoch().count();
    return mills;
}

bool FileSystem::moveTo(const char* fromFile, const char* toFile) {
    std::error_code ec;
    fs::rename(fromFile, toFile, ec);
    return ec.value() == 0;
}

bool FileSystem::copyTo(const char* fromFile, const char* toFile) {
    const auto copyOptions = fs::copy_options::update_existing | fs::copy_options::overwrite_existing;

    std::error_code ec;
    fs::copy_file(fromFile, toFile, copyOptions, ec);
    return ec.value() == 0;
}

bool FileSystem::remove(const char* filePath) {
    std::error_code ec;
    fs::remove_all(fs::u8path(filePath), ec);
    return ec.value() == 0;
}

bool FileSystem::isAbsolute(const char* filePath)
{
    return fs::path(filePath).is_absolute();
}

String FileSystem::canonicalPath(const char* filePath) {
    return fs::canonical(filePath).string();
}

String FileSystem::fileName(const char* path) {
    return fs::path(path).filename().string();
}

String FileSystem::getExtName(const char* path)
{
    return fs::path(path).extension().string();
}

String FileSystem::getParentPath(const char* path) {
    return fs::path(path).parent_path().string();
}

String FileSystem::getBaseName(const char* path) {
    return fs::path(path).stem().string();
}

}
