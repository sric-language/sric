#include "sric/Stream.h"

using namespace sric;

IOStream::IOStream(): littleEndian(true) {
}

bool IOStream::rewind() {
    if (canSeek()) {
        return seek(0);
    }
    return false;
}

bool IOStream::eof() {
    return position() >= length();
}

long IOStream::pipeTo(IOStream* out) {
    long size = 0;
    long succSize = 0;
    char buffer[1024];
    long err;

    while (true) {
        size = this->read(buffer, 1024);
        if (size <= 0) {
            break;
        }
        err = out->write(buffer, size);
        if (err != -1) {
            return succSize;
        }
        succSize += size;
    }

    return succSize;
}

long IOStream::writeUInt8(uint8_t out) {
    static const int size = 1;
    uint8_t data[size];
    data[0] = out;
    long s = this->write((char*)data, size);
    return s;
}

long IOStream::writeUInt16(uint16_t out) {
    static const int size = 2;
    unsigned char data[size];

    if (!littleEndian) {
        data[0] = (out >> 8) & 0xff;
        data[1] = (out) & 0xff;
    }
    else {
        data[1] = (out >> 8) & 0xff;
        data[0] = (out) & 0xff;
    }

    long s = this->write((char*)data, size);
    return s;
}

long IOStream::writeUInt32(uint32_t out) {
    static const int size = 4;
    unsigned char data[size];
    if (!littleEndian) {
        data[0] = (out >> 24) & 0xff;
        data[1] = (out >> 16) & 0xff;
        data[2] = (out >> 8) & 0xff;
        data[3] = (out) & 0xff;
    }
    else {
        data[3] = (out >> 24) & 0xff;
        data[2] = (out >> 16) & 0xff;
        data[1] = (out >> 8) & 0xff;
        data[0] = (out) & 0xff;
    }
    long s = this->write((char*)data, size);
    return s;
}

long IOStream::writeUInt64(uint64_t out) {
    static const int size = 8;
    unsigned char data[size];
    if (!littleEndian) {
        data[0] = (out >> 56) & 0xff;
        data[1] = (out >> 48) & 0xff;
        data[2] = (out >> 40) & 0xff;
        data[3] = (out >> 32) & 0xff;
        data[4] = (out >> 24) & 0xff;
        data[5] = (out >> 16) & 0xff;
        data[6] = (out >> 8) & 0xff;
        data[7] = (out) & 0xff;
    }
    else {
        data[7] = (out >> 56) & 0xff;
        data[6] = (out >> 48) & 0xff;
        data[5] = (out >> 40) & 0xff;
        data[4] = (out >> 32) & 0xff;
        data[3] = (out >> 24) & 0xff;
        data[2] = (out >> 16) & 0xff;
        data[1] = (out >> 8) & 0xff;
        data[0] = (out) & 0xff;
    }
    long s = this->write((char*)data, size);
    return s;
}

uint8_t IOStream::readUInt8() {
    static const int size = 1;
    unsigned char data[size];
    long s = this->read((char*)data, size);
    if (s < size) return -1;
    return data[0];
}

uint16_t IOStream::readUInt16() {
    static const int size = 2;
    unsigned char data[size];
    long s = this->read((char*)data, size);
    if (s < size) return -1;
    uint16_t byte1 = data[0];
    uint16_t byte2 = data[1];

    if (!littleEndian) {
        return ((byte1 << 8) | byte2);
    }
    else {
        return ((byte2 << 8) | byte1);
    }
}

uint32_t IOStream::readUInt32() {
    static const int size = 4;
    unsigned char data[size];
    long s = this->read((char*)data, size);
    if (s < size) return -1;

    uint32_t byte1 = data[0];
    uint32_t byte2 = data[1];
    uint32_t byte3 = data[2];
    uint32_t byte4 = data[3];

    if (!littleEndian) {
        return ((byte1 << 24) | (byte2 << 16) | (byte3 << 8) | byte4);
    }
    else {
        return ((byte4 << 24) | (byte3 << 16) | (byte2 << 8) | byte1);
    }
}

uint64_t IOStream::readUInt64() {
    static const int size = 8;
    unsigned char data[size];
    long s = this->read((char*)data, size);
    if (s < size) return -1;

    uint64_t byte1 = data[0];
    uint64_t byte2 = data[1];
    uint64_t byte3 = data[2];
    uint64_t byte4 = data[3];
    uint64_t byte5 = data[4];
    uint64_t byte6 = data[5];
    uint64_t byte7 = data[6];
    uint64_t byte8 = data[7];

    if (!littleEndian) {
        return ((byte1 << 56) | (byte2 << 48) | (byte3 << 40) | (byte4 << 32)
            | (byte5 << 24) | (byte6 << 16) | (byte7 << 8) | byte8);
    }
    else {
        return ((byte8 << 56) | (byte7 << 48) | (byte6 << 40) | (byte5 << 32)
            | (byte4 << 24) | (byte3 << 16) | (byte2 << 8) | byte1);
    }
}

long IOStream::writeInt8(int8_t out) {
    uint8_t intVal = *((uint8_t*)&out);
    return writeUInt8(intVal);
}

long IOStream::writeInt16(int16_t out) {
    uint16_t intVal = *((uint16_t*)&out);
    return writeUInt16(intVal);
}
long IOStream::writeInt32(int32_t out) {
    uint32_t intVal = *((uint32_t*)&out);
    return writeUInt32(intVal);
}
long IOStream::writeInt64(int64_t out) {
    uint64_t intVal = *((uint64_t*)&out);
    return writeUInt64(intVal);
}
long IOStream::writeFloat(float out) {
    uint32_t intVal = *((uint32_t*)&out);
    return writeUInt32(intVal);
}
long IOStream::writeDouble(double out) {
    uint64_t intVal = *((uint64_t*)&out);
    return writeUInt64(intVal);
}

int8_t IOStream::readInt8() {
    uint8_t intVal = this->readUInt8();
    return *((int8_t*)&intVal);
}

int16_t IOStream::readInt16() {
    uint16_t intVal = this->readUInt16();
    return *((int16_t*)&intVal);
}

int32_t IOStream::readInt32() {
    uint32_t intVal = this->readUInt32();
    return *((int32_t*)&intVal);
}

int64_t IOStream::readInt64() {
    uint64_t intVal = this->readUInt64();
    return *((int64_t*)&intVal);
}

float IOStream::readFloat32() {
    uint32_t intVal = this->readUInt32();
    return *((float*)&intVal);
}

double IOStream::readFloat64() {
    uint64_t intVal = this->readUInt64();
    return *((double*)&intVal);
}

///////////////////////////////////////////////////////////


void IOStream::writeSizedStr(const String& buf) {
    size_t size = buf.size();
    writeUInt32((uint32_t)size);
    write(buf.c_str(), size);
}

String IOStream::readSizedStr() {
    size_t size = readUInt32();
    std::string s;
    s.resize(size);
    read((char*)s.data(), size);
    return (s);
}

void IOStream::writeStr(const String& buf) {
    int size = buf.size();
    write(buf.c_str(), size);
}
String IOStream::readAllStr() {
    long size = this->length();
    String s;
    s.resize(size);
    read((char*)s.data(), size);
    return (s);
}

char* IOStream::readLine(char* str, int bufSize)
{
    if (bufSize <= 0)
        return NULL;
    char c = 0;
    size_t maxCharsToRead = bufSize - 1;
    for (size_t i = 0; i < maxCharsToRead; ++i)
    {
        size_t result = read(&c, 1);
        if (result != 1)
        {
            str[i] = '\0';
            break;
        }
        if (c == '\n')
        {
            str[i] = '\0';
            break;
        }
        else if(c == '\r')
        {
            str[i] = '\0';
            // next may be '\n'
            size_t pos = position();
            char nextChar = 0;
            if (read(&nextChar, 1) != 1)
            {
                // no more characters
                break;
            }
            if (nextChar != '\n')
            {
                seek(pos);
            }
            break;
        }
        str[i] = c;
    }
    return str; // what if first read failed?
}
