#ifndef SRIC_FILESTREAM_H_
#define SRIC_FILESTREAM_H_

#include "sric/Stream.h"
#include <string>
#include "sric/OwnPtr.h"

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
    virtual uint32_t read(void* ptr, size_t size);
    //virtual char* readLine(char* str, int num);
    virtual uint32_t write(const void* ptr, size_t size);
    virtual bool eof();
    virtual uint32_t length();
    virtual uint32_t position();
    virtual bool seek(uint32_t offset);
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