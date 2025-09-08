/*
 * Copyright (c) 2023, chunquedong
 *
 * This file is part of mgpPro project
 * all rights reserved
 *
 */
#include "HttpClientX.h"

#include <assert.h>
#if __EMSCRIPTEN__
    #include <emscripten.h>
    #include <emscripten/html5.h>
    #include <emscripten/fetch.h>
#endif

using namespace sricNet;

#ifdef __cplusplus
extern "C" {
#endif
#include "base64.h"
#ifdef __cplusplus
}
#endif

#ifdef __ANDROID__
std::string g_cacheFilePath = "/sdcard/cache/";
#elif defined(__EMSCRIPTEN__)
std::string g_cacheFilePath = "cache";
#else
std::string g_cacheFilePath = "cache/";
#endif

static std::string nameEncode(const std::string& data) {
    std::string encode_out;
    encode_out.resize(BASE64_ENCODE_OUT_SIZE(data.size()));
    base64_encode((const unsigned char*)data.data(), data.size(), (char*)encode_out.data());
    if (encode_out.size() > 0 && encode_out[encode_out.size() - 1] == 0) {
        encode_out.resize(encode_out.size() - 1);
    }
    return encode_out;
}

void HttpClient::initCachePath(const char* dir) {
    g_cacheFilePath = dir;
    assert(g_cacheFilePath[g_cacheFilePath.size() - 1] == '/');
    if (!sric::FileSystem::exists(g_cacheFilePath.c_str())) {
        sric::FileSystem::mkdirs(g_cacheFilePath.c_str());
    }
}

bool HttpClient::isNetUrl(const char* url) {
#if __EMSCRIPTEN__
    bool fromNet = true;
#else
    bool fromNet = false;
#endif
    const sric::String& uri = url;
    //local file
    if (uri.size() > 2 && uri[1] == ':') {
        fromNet = false;
    }
    else if (uri.startsWith("res/")) {
        fromNet = false;
    }
    else if (uri.startsWith("http")) {
        fromNet = true;
    }
    return fromNet;
}

#ifndef __EMSCRIPTEN__

bool HttpClient::send() {
    if (strcmp(method.c_str(), "GET") != 0) {
        useCache = false;
    }

    if (useCache) {
        if (cacheFile.size() == 0) {
            if (!isNetUrl(url.c_str())) {
                _localFile = true;
                cacheFile = url.copy();
            }
        }
        if (cacheFile.size() == 0) {
            cacheFile = "net/" + nameEncode(url);
        }

        std::string cacheFile = _localFile ? this->cacheFile.cpp_str() : g_cacheFilePath + this->cacheFile.cpp_str();
        if (sric::FileSystem::exists(cacheFile.c_str())) {
            auto stream = sric::FileStream::open(cacheFile.c_str(), "rb");
            _response.url = url.copy();
            _response.id = id;
            _response.cacheFile = cacheFile;
            _response.statusCode = -1;
            _response.result = stream->readAllStr();
            _fromCache = true;
            stream->close();
            doReceive();
            return true;
        }
    }
    RawHttpClient::send();
    return true;
}

void HttpClient::doReceive() {
    if (!_fromCache && !_error && useCache) {
        std::string cacheFile = _localFile ? this->cacheFile.cpp_str() : g_cacheFilePath + this->cacheFile.cpp_str();
        std::string dir = sric::FileSystem::getParentPath(cacheFile.c_str());
        if (!sric::FileSystem::exists(dir.c_str())) {
            sric::FileSystem::mkdirs(dir.c_str());
        }
        auto stream = sric::FileStream::open(cacheFile.c_str(), "wb");
        stream->write(_response.result.data(), _response.result.size());
        stream->close();
        //delete stream;
        _response.cacheFile = cacheFile;
    }
    RawHttpClient::doReceive();
}

#else


static void idb_on_store_callback_func(void*) {
    //printf("save idb sucess\n");
}

static void idb_on_store_error_callback_func(void*) {
    printf("save idb error\n");
}

static void idb_async_wget_onload_func(void* arg, void* buf, int size) {
    HttpClient* self = (HttpClient*)arg;

    //printf("load idb success %s %d\n", self->cacheFile.c_str(), size);

    self->_response.statusCode = -1;
    sric::String data((const char*)buf, size);
    self->_response.result = std::move(data);

    self->_fromCache = true;
    self->doReceive();
}

static void idb_load_error_callback_func(void* arg) {
    HttpClient* self = (HttpClient*)arg;

    //printf("load idb error %s\n", self->cacheFile.c_str());

    self->RawHttpClient::send();
}

bool HttpClient::send() {
    if (method != "GET") {
        useCache = false;
    }
    if (useCache) {
        if (cacheFile.size() == 0) {
            cacheFile = "net/" + nameEncode(url);
        }

        emscripten_idb_async_load(g_cacheFilePath.c_str(), cacheFile.c_str(), this, idb_async_wget_onload_func, idb_load_error_callback_func);
    }
    return true;
}

void HttpClient::doReceive() {
    if (!_fromCache && !_error && useCache) {
        //responseDataCopy = _response.result;
        emscripten_idb_async_store(g_cacheFilePath.c_str(), cacheFile.c_str(), _response.result.data(), _response.result.size(), this, idb_on_store_callback_func, idb_on_store_error_callback_func);
    }
    RawHttpClient::doReceive();
}
#endif


//
//int main() {
//    {
//        sric::OwnPtr<HttpClient> client = sric::makePtr<HttpClient>();
//        client->url = "http://www.baidu.com";
//        client->send();
//    }
//    sric::sleep(15000);
//    return 0;
//}