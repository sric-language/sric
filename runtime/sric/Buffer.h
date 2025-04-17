/*
 * Copyright (c) 2012-2016, chunquedong
 *
 * This file is part of cppfan project
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE version 3.0
 *
 * History:
 *   2012-12-23  Jed Young  Creation
 */
#ifndef SRIC_BUFFER_H_
#define SRIC_BUFFER_H_

#include "sric/Stream.h"

namespace sric
{

/**
 * ByteArray is memory buffer
 */
class Buffer : public IOStream {
private:
    uint8_t* data;
    size_t _pos;
    size_t _size;
    bool owner;
public:
    Buffer();
    Buffer(size_t size);
    Buffer(uint8_t* data, size_t size, bool owner);
    ~Buffer();

    static OwnPtr<Buffer> make(size_t size);

    void readSlice(Buffer &out, bool copy);

    //read Data no copy
    unsigned char * readDirect(int len);
    unsigned char *getData() { return data; }

    virtual long read(void* ptr, size_t size);
    virtual long write(const void* ptr, size_t size);
    virtual long length() { return _size; }
    virtual long position() { return _pos; }
    virtual bool seek(long int offset);
};


}

#endif