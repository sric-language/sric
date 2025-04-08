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
class FileStream : public IOStream
{
public:
    
    ~FileStream();
    virtual bool canRead();
    virtual bool canWrite();
    virtual bool canSeek();
    virtual void close();
    virtual long read(void* ptr, size_t size);
    //virtual char* readLine(char* str, int num);
    virtual long write(const void* ptr, size_t size);
    virtual bool eof();
    virtual long length();
    virtual long position();
    virtual bool seek(long int offset);
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