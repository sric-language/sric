/*
 * Copyright (c) 2012-2016, chunquedong
 *
 * This file is part of cppfan project
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE version 3.0
 *
 * History:
 *   2012-12-23  Jed Young  Creation
 */
#include "sric/Buffer.h"

using namespace sric;

Buffer::Buffer() :
    data(nullptr), _pos(0), _size(0), owner(false) {
}

Buffer::Buffer(size_t size) :  _pos(0), _size(size), owner(true) {
  data = (uint8_t*)malloc(size);
}

Buffer::Buffer(uint8_t* data, size_t size, bool owner) :
    data(data), _pos(0), _size(size), owner(owner) {
}

Buffer::~Buffer() {
  if (owner) {
    free(data);
  }
}

OwnPtr<Buffer> Buffer::make(size_t size) {
    OwnPtr<Buffer> stream = sric::new_<Buffer>();
    stream->data = (uint8_t*)malloc(size);
    stream->_size = size;
    stream->owner = true;
    return stream;
}

uint32_t Buffer::write(const void* ptr, size_t size) {
  size_t len = size;
  if (len > _size - _pos) {
    if (owner) {
      uint8_t *p = (uint8_t*)realloc(data, _pos+ len);
      if (p) {
        data = p;
        _size = _pos + len;
      }
    } else {
        len = _size - _pos;
    }
  }
  memcpy(data + _pos, ptr, len);
  _pos += len;
  return size;
}

uint32_t Buffer::read(void* ptr, size_t size) {
  if (size > remaining()) {
    size = remaining();
  }
  size_t len = size;
  memcpy(ptr, data + _pos, len);
  _pos += len;
  return size;
}

unsigned char * Buffer::readDirect(int len) {
  unsigned char *p = data+_pos;
  if (_pos <= _size - len) {
    _pos += len;
    return p;
  }
  else {
    _pos = _size;
    return p;
  }
}

void Buffer::readSlice(Buffer &out, bool copy) {
    out._pos = 0;
    size_t size = readUInt16();
    out._size = size;
    if (_pos + size >= _size) {
        return;
    }
    uint8_t *data = readDirect((int)size);
    if (copy) {
        out.data = (uint8_t*)malloc(size);
        memcpy(out.data, data, size);
        out.owner = true;
    } else {
        out.data = data;
        out.owner = false;
    }
}

bool Buffer::seek(uint32_t pos) {
    if (pos <= _size) {
        this->_pos = pos;
        return true;
    }
    return false;
}
