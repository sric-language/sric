#ifndef SRIC_FILESTREAM_H_
#define SRIC_FILESTREAM_H_

#include "Stream.h"
#include <string>
#include "Ptr.h"

namespace sric
{

/**
 * 
 * @script{ignore}
 */
class FileStream : public Stream
{
public:
    
    ~FileStream();
    virtual bool canRead();
    virtual bool canWrite();
    virtual bool canSeek();
    virtual void close();
    virtual size_t read(void* ptr, size_t size, size_t count);
    virtual char* readLine(char* str, int num);
    virtual size_t write(const void* ptr, size_t size, size_t count);
    virtual bool eof();
    virtual size_t length();
    virtual long int position();
    virtual bool seek(long int offset, int origin = SEEK_SET);
    virtual bool rewind();

    static OwnPtr<FileStream> open(const char* filePath, const char* mode);


    FileStream();
    FileStream(FILE* file);

private:
    FILE* _file;
    bool _canRead;
    bool _canWrite;
};

}

#endif