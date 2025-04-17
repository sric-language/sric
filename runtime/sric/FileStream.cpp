#include "sric/FileStream.h"

using namespace sric;

FileStream::FileStream()
    : _file(NULL), _canRead(false), _canWrite(false) {

}

FileStream::FileStream(FILE* file)
    : _file(file), _canRead(false), _canWrite(false)
{
    
}

FileStream::~FileStream()
{
    if (_file)
    {
        close();
    }
}

#ifdef  _WIN32
#include <Windows.h>
std::string Utf8ToGbk(const char* src_str)
{
    int len = MultiByteToWideChar(CP_UTF8, 0, src_str, -1, NULL, 0);
    wchar_t* wszGBK = new wchar_t[len + 1];
    memset(wszGBK, 0, len * 2 + 2);
    MultiByteToWideChar(CP_UTF8, 0, src_str, -1, wszGBK, len);
    len = WideCharToMultiByte(CP_ACP, 0, wszGBK, -1, NULL, 0, NULL, NULL);
    char* szGBK = new char[len + 1];
    memset(szGBK, 0, len + 1);
    WideCharToMultiByte(CP_ACP, 0, wszGBK, -1, szGBK, len, NULL, NULL);
    std::string strTemp(szGBK);
    if (wszGBK) delete[] wszGBK;
    if (szGBK) delete[] szGBK;
    return strTemp;
}
#endif //  _WIN32


OwnPtr<FileStream> FileStream::open(const char* filePath, const char* mode)
{
#ifdef  _WIN32
    FILE* file = fopen(Utf8ToGbk(filePath).c_str(), mode);
#else
    FILE* file = fopen(filePath, mode);
#endif
    if (file)
    {
        OwnPtr<FileStream> stream = sric::new_<FileStream>();
        stream->_file = file;
        const char* s = mode;
        while (s != NULL && *s != '\0')
        {
            if (*s == 'r')
                stream->_canRead = true;
            else if (*s == 'w')
                stream->_canWrite = true;
            ++s;
        }

        return stream;
    }
    else {
        fprintf(stderr, "open file fail:%s", filePath);
    }
    return OwnPtr<FileStream>();
}

bool FileStream::canRead()
{
    return _file && _canRead;
}

bool FileStream::canWrite()
{
    return _file && _canWrite;
}

bool FileStream::canSeek()
{
    return _file != NULL;
}

void FileStream::close()
{
    if (_file)
        fclose(_file);
    _file = NULL;
}

long FileStream::read(void* ptr, size_t size)
{
    if (!_file)
        return 0;
    return fread(ptr, 1, size, _file);
}

//char* FileStream::readLine(char* str, int num)
//{
//    if (!_file)
//        return 0;
//    return fgets(str, num, _file);
//}

long FileStream::write(const void* ptr, size_t size)
{
    if (!_file)
        return 0;
    return fwrite(ptr, 1, size, _file);
}

bool FileStream::eof()
{
    if (!_file || feof(_file))
        return true;
    return ((size_t)position()) >= length();
}

long FileStream::length()
{
    size_t len = 0;
    if (canSeek())
    {
        long int pos = position();
        fseek(_file, 0, SEEK_END);
        len = position();
        fseek(_file, pos, SEEK_SET);
    }
    return len;
}

long int FileStream::position()
{
    if (!_file)
        return -1;
    return ftell(_file);
}

bool FileStream::seek(long int offset)
{
    if (!_file)
        return false;
    return fseek(_file, offset, SEEK_SET) == 0;
}

bool FileStream::rewind()
{
    if (canSeek())
    {
        ::rewind(_file);
        return true;
    }
    return false;
}
