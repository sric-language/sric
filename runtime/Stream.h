#ifndef SRIC_Stream_H_
#define SRIC_Stream_H_

#include <stdint.h>
#include "Ptr.h"
#include "Str.h"

namespace sric
{

class IOStream
{
public:
    virtual ~IOStream() {};

    virtual bool canRead() { return true; }

    virtual bool canWrite() { return true; }

    virtual bool canSeek() { return true; }
    
    virtual char* readLine(char* buf, int bufSize);
    
    virtual long read(void* ptr, size_t size) = 0;

    virtual long write(const void* ptr, size_t size) = 0;

    virtual bool eof();

    virtual long length() = 0;

    virtual long position() = 0;

    virtual bool seek(long int offset) = 0;

    virtual bool rewind();

    long remaining() { return length() - position(); }

protected:
    IOStream();
private:
    IOStream(const IOStream&) = delete;            // Hidden copy constructor.
    IOStream& operator=(const IOStream&) = delete; // Hidden copy assignment operator.

    bool littleEndian = true;
public:
    void setByteOrder(bool t) { littleEndian = t; };
    bool getByteOrder() { return littleEndian; }

    virtual void flush() {}

    virtual void close() {}

    virtual long pipeTo(IOStream* out);

    long writeInt8(int8_t out);
    long writeInt16(int16_t out);
    long writeInt32(int32_t out);
    long writeInt64(int64_t out);
    long writeUInt8(uint8_t out);
    long writeUInt16(uint16_t out);
    long writeUInt32(uint32_t out);
    long writeUInt64(uint64_t out);
    long writeFloat(float out);
    long writeDouble(double out);

    uint8_t readUInt8();
    uint16_t readUInt16();
    uint32_t readUInt32();
    uint64_t readUInt64();
    int8_t readInt8();
    int16_t readInt16();
    int32_t readInt32();
    int64_t readInt64();
    float readFloat32();
    double readFloat64();

    void writeSizedStr(const String& buf);
    String readSizedStr();

    void writeStr(const String& buf);
    String readAllStr();
};


}

#endif
