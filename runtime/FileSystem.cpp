
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

#include "FileSystem.h"
#include "Stream.h"
#include "FileStream.h"

#include <algorithm>
#include <assert.h>
#include <mutex>

namespace sric
{
static std::mutex __fileLock;

namespace fs = std::filesystem;

bool FileSystem::mkdirs(const char* cpath) {
    //printf("mkdirs: %s\n", cpath);
    std::lock_guard<std::mutex> guard(__fileLock);

    std::vector<std::string> dirs;
    std::string path = cpath;
    std::string dirPath;
    while (path.length() > 0)
    {
        int index = path.find('/');
        if (index == -1) {
            dirs.push_back(path);
        }
        else if (index == 0) {
            dirPath = "/";
        }
        else {
            dirs.push_back(path.substr(0, index));
        }

        if (index + 1 >= path.length() || index == -1)
            break;

        path = path.substr(index + 1);
    }

    for (unsigned int i = 0; i < dirs.size(); i++)
    {
        if (i > 0) dirPath += "/";
        dirPath += dirs[i];
        //printf("mkdir: %s\n", dirPath.c_str());
#ifdef _WIN32
        DWORD rc = GetFileAttributesA(dirPath.c_str());
        if (rc == INVALID_FILE_ATTRIBUTES) {
            if (CreateDirectoryA(dirPath.c_str(), NULL) == 0) {
                printf("Failed to create directory: '%s'", dirPath.c_str());
                return false;
            }
        }
#else
        struct stat s;
        if (stat(dirPath.c_str(), &s) != 0)
        {
            // Directory does not exist.
            if (mkdir(dirPath.c_str(), 0777) != 0)
            {
                printf("Failed to create directory: '%s'", dirPath.c_str());
                return false;
            }
        }
#endif
    }
    return false;
}

bool FileSystem::listFiles(const char* dirPath, RefPtr<DArray<String>> files)
{
    fs::path dir = fs::u8path(dirPath);
    for (auto& p : fs::directory_iterator(dir)) {
        std::string pathstr = p.path().string();
        files->add(pathstr);
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

bool FileSystem::isAbsolutePath(const char* filePath)
{
    if (filePath == 0 || filePath[0] == '\0')
        return false;
#ifdef _WIN32
    if (filePath[1] != '\0')
    {
        char first = filePath[0];
        return (filePath[1] == ':' && ((first >= 'a' && first <= 'z') || (first >= 'A' && first <= 'Z')));
    }
    return false;
#else
    return filePath[0] == '/';
#endif
}

String FileSystem::getDirName(const char* path)
{
    if (path == NULL || strlen(path) == 0)
    {
        return "";
    }
#ifdef _WIN32
    char drive[_MAX_DRIVE];
    char dir[_MAX_DIR];
    _splitpath(path, drive, dir, NULL, NULL);
    std::string dirname;
    size_t driveLength = strlen(drive);
    if (driveLength > 0)
    {
        dirname.reserve(driveLength + strlen(dir));
        dirname.append(drive);
        dirname.append(dir);
    }
    else
    {
        dirname.assign(dir);
    }
    std::replace(dirname.begin(), dirname.end(), '\\', '/');
    return dirname;
#else
    // dirname() modifies the input string so create a temp string
    std::string dirname;
    char* tempPath = new char[strlen(path) + 1];
    strcpy(tempPath, path);
    char* dir = ::dirname(tempPath);
    if (dir && strlen(dir) > 0)
    {
        dirname.assign(dir);
        // dirname() strips off the trailing '/' so add it back to be consistent with Windows
        dirname.append("/");
    }
    SAFE_DELETE_ARRAY(tempPath);
    return dirname;
#endif
}

String FileSystem::getExtName(const char* path)
{
    const char* str = strrchr(path, '.');
    if (str == NULL)
        return "";

    std::string ext;
    size_t len = strlen(str);
    for (size_t i = 1; i < len; ++i)
        //if (uppper) ext += std::toupper(str[i]);
        //else
        ext += str[i];

    return ext;
}

String FileSystem::getParentPath(const char* path) {
    std::string spath = path;
#if _WIN32
    std::replace(spath.begin(), spath.end(), '\\', '/');
#endif

    std::string::size_type pos = spath.find_last_of("/");
    if (pos != std::string::npos && pos + 1 < spath.size()) {
        spath = spath.substr(0, pos);
    }
    return spath;
}

String FileSystem::getBaseName(const char* path) {
    std::string spath = path;
#if _WIN32
    std::replace(spath.begin(), spath.end(), '\\', '/');
#endif

    std::string::size_type pos = spath.find_last_of("/");
    if (pos != std::string::npos && pos+1 < spath.size()) {
        spath = spath.substr(pos + 1);
    }

    pos = spath.find_last_of(".");
    if (pos != std::string::npos) {
        spath = spath.substr(0, pos);
    }
    return spath;
}

}
