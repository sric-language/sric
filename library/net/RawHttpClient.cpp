/*
 * Copyright (c) 2023, chunquedong
 *
 * This file is part of mgpPro project
 * all rights reserved
 *
 */
#include "RawHttpClient.hpp"

#include <assert.h>
#if __EMSCRIPTEN__
    #include <emscripten.h>
    #include <emscripten/html5.h>
    #include <emscripten/fetch.h>
#else
    #include "curl/curl.h"
#endif

using namespace sricNet;

RawHttpClient::RawHttpClient() {
}

RawHttpClient::~RawHttpClient() {
}

float RawHttpClient::getProgress() {
    return _progress.load();
}

void RawHttpClient::doReceive() {
    if (!_error && !_cancel) {
        _progress = 1;
    }

    if (onDecode && onReceive) {
        if (!_error && !_cancel && (_response.statusCode < 300 || _response.statusCode == -1)) {
            _response.decodeResult = onDecode(_response);
        }
        else {
            printf("net error:%s, %d, %d\n", url.c_str(), _response.statusCode, _error);
        }
    }

    if (onReceive) {
        if (sric::call_later) {
            sric::OwnPtr<RawHttpClient> self = sric::rawToOwn(this);
            sric::AutoMove<sric::OwnPtr<RawHttpClient> > autoMove(self);
            sric::call_later([=] {    
                if (autoMove.get()->onReceive) {
                    autoMove.get()->onReceive(autoMove.get()->_response);
                    //printf("call onReceive\n");
                } 
            });
        }
        else {
            onReceive(_response);
        }
    }
}

//////////////////////////////////////////////////////////////////////////////////////////////////////////
// CURL
//////////////////////////////////////////////////////////////////////////////////////////////////////////

#ifndef __EMSCRIPTEN__

sric::ThreadPool<sric::SharedPtr<RawHttpClient> >* g_httpThreadPool;

void RawHttpClient::cancel() {
    _cancel = true;
}

bool RawHttpClient::send() {
    sric::OwnPtr<RawHttpClient> self = sric::rawToOwn(this);
    if (!g_httpThreadPool) {
        g_httpThreadPool = new sric::ThreadPool<sric::SharedPtr<RawHttpClient> >(4);
        g_httpThreadPool->start();
    }
    g_httpThreadPool->addTask(sric::toShared(self));
    return true;
}

static int progress_callback(void* clientp,
    double dltotal,
    double dlnow,
    double ultotal,
    double ulnow) {
    RawHttpClient* me = (RawHttpClient*)clientp;
    if (dltotal == 0) {
        me->_progress = 1;
    }
    else {
        me->_progress = dlnow / dltotal;
    }
    if (me->isCanceled()) {
        return CURLE_ABORTED_BY_CALLBACK;
    }
    return CURL_PROGRESSFUNC_CONTINUE;
}

static size_t OnWriteData(char* ptr, size_t size, size_t nmemb, void* userdata) {
    RawHttpClient* me = (RawHttpClient*)userdata;
    unsigned long asize = size * nmemb;
    me->_response.result.addData(ptr, asize);
    return asize;
}

void RawHttpClient::run() {
    _response.url = url.copy();
    _response.id = id;

    printf("reqeust: %s\n", url.c_str());

    CURL* curl = curl_easy_init();
    _curl = curl;

    struct curl_slist* cheaders = NULL;
    if (headers.size() > 0) {
        for (auto it = headers.begin(); it != headers.end(); ++it) {
            std::string item = it->first + ":" + it->second;
            cheaders = curl_slist_append(cheaders, item.c_str());
        }
    }
    if (cheaders != NULL) {
        curl_easy_setopt(curl, CURLOPT_HTTPHEADER, cheaders);
    }

    sric::String eurl = url.copy();
    eurl.replace(" ", "%20");
    curl_easy_setopt(curl, CURLOPT_URL, eurl.c_str());
    if (this->method == sric::asStr("POST")) {
        curl_easy_setopt(curl, CURLOPT_POST, 1);
        curl_easy_setopt(curl, CURLOPT_POSTFIELDS, this->content.c_str());
    }

    curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, OnWriteData);
    curl_easy_setopt(curl, CURLOPT_WRITEDATA, this);
    curl_easy_setopt(curl, CURLOPT_NOSIGNAL, 1);
    curl_easy_setopt(curl, CURLOPT_USERAGENT, "Mozilla/5.0");
    curl_easy_setopt(curl, CURLOPT_FOLLOWLOCATION, 1);
    curl_easy_setopt(curl, CURLOPT_NOPROGRESS, 0L);
    curl_easy_setopt(curl, CURLOPT_XFERINFODATA, this);
    curl_easy_setopt(curl, CURLOPT_XFERINFOFUNCTION, progress_callback);
    curl_easy_setopt(curl, CURLOPT_VERBOSE, 0);

    //curl_easy_setopt(curl, CURLOPT_SSL_VERIFYPEER, false);
    //curl_easy_setopt(curl, CURLOPT_SSL_VERIFYHOST, false);

    CURLcode res = curl_easy_perform(curl);
    if (cheaders != NULL) {
        curl_slist_free_all(cheaders); //free the list again
    }
    _response.statusCode = 0;
    if (res == CURLE_OK) {
        curl_easy_getinfo(curl, CURLINFO_RESPONSE_CODE, &_response.statusCode);
        //resultStream.rewind();
    }
    else {
        _error = 1;
    }

    curl_easy_cleanup(curl);

    doReceive();
}

#else


//////////////////////////////////////////////////////////////////////////////////////////////////////////
// Fetch API
//////////////////////////////////////////////////////////////////////////////////////////////////////////

void RawHttpClient::cancel() {
    _cancel = true;
    if (_handle) {
        // _isClosed = true;
        if (!_useFetchApi) {
            emscripten_async_wget2_abort((int)_handle);
            _handle = NULL;
        }
        else {
            emscripten_fetch_t* f = (emscripten_fetch_t*)_handle;
            _handle = NULL;
            //printf("cancel:%p, %p\n", f, f->userData);
            //f->userData = nullptr;
            //fetch api support cancel?
            //emscripten_fetch_close(f);
        }
    }
}

static void downloadSucceeded(emscripten_fetch_t *fetch) {
    if (!fetch->userData) {
        return;
    }
    //printf("Finished downloading %llu bytes from URL %s.\n", fetch->numBytes, fetch->url);
    // The data is now available at fetch->data[0] through fetch->data[fetch->numBytes-1];
    //printf("downloadSucceeded:%p, %p\n", fetch, fetch->userData);

    RawHttpClient* self = (RawHttpClient*)fetch->userData;
    if (self->_cancel) {
        fetch->userData = NULL;
        emscripten_fetch_close(fetch);
        sric::dealloc(self);
        return;
    }
    // if (self->_isClosed) {
    //     return;
    // }
    self->_response.statusCode = 200;
    sric::String data((const char*)fetch->data, fetch->numBytes);
    self->_response.result = std::move(data);

    self->doReceive();

    self->_handle = NULL;
    // self->_isClosed = true;
    emscripten_fetch_close(fetch); // Free data associated with the fetch.

    fetch->userData = NULL;
    sric::dealloc(self);
}

static void downloadFailed(emscripten_fetch_t *fetch) {
    if (!fetch->userData) {
        return;
    }
    //printf("downloadFailed:%p, %p\n", fetch, fetch->userData);
    //printf("Downloading %s failed, HTTP failure status code: %d.\n", fetch->url, fetch->status);

    RawHttpClient* self = (RawHttpClient*)fetch->userData;
    if (self->_cancel) {
        fetch->userData = NULL;
        emscripten_fetch_close(fetch);
        sric::dealloc(self);
        return;
    }
    // if (self->_isClosed) {
    //     return;
    // }
    self->_response.statusCode = fetch->status;
    self->_error = true;

    self->doReceive();

    self->_handle = NULL;
    // self->_isClosed = true;
    emscripten_fetch_close(fetch); // Free data associated with the fetch.

    fetch->userData = NULL;
    sric::dealloc(self);
}

static void downloadProgress(emscripten_fetch_t *fetch) {
    if (!fetch->userData) {
        return;
    }
    RawHttpClient* me = (RawHttpClient*)fetch->userData;
    if (fetch->totalBytes == 0) {
        //me->_progress = 1;
    }
    else {
        me->_progress = fetch->dataOffset / (float)fetch->totalBytes;
    }
}

//////////////////////////////////////////////////////////////////////////////////////////////////////////
// Async wget API
//////////////////////////////////////////////////////////////////////////////////////////////////////////

static void async_wget2_onload_func(unsigned int handle, void* arg, void* buf, unsigned int size) {
    RawHttpClient* self = (RawHttpClient*)arg;
    self->_response.statusCode = 200;
    sric::String data((const char*)buf, size);
    self->_response.result = std::move(data);

    self->doReceive();
    sric::dealloc(self);
}

static void async_wget2_onerror_func(unsigned int handle, void* arg, int statusCode, const char* msg) {
    RawHttpClient* self = (RawHttpClient*)arg;
    self->_response.statusCode = statusCode;
    self->_error = true;

    self->doReceive();
    sric::dealloc(self);
}

static void async_wget2_onprogress_func(unsigned int handle, void* arg, int bytes, int total) {
    RawHttpClient* me = (RawHttpClient*)arg;
    if (total == 0) {
        //me->_progress = 1;
    }
    else {
        me->_progress = bytes /(float)total;
    }
}

//////////////////////////////////////////////////////////////////////////////////////////////////////////
// Entry point
//////////////////////////////////////////////////////////////////////////////////////////////////////////

bool RawHttpClient::send() {

    //fetch api not support cancel and async_wget api not support headers
    if (headers.size() > 0) {
        _useFetchApi = true;
    }

    _response.url = url.copy();
    _response.id = id;

    sric::OwnPtr<RawHttpClient> selfOwn = sric::rawToOwn(this);
    RawHttpClient* self = selfOwn.take();
    
    sric::String eurl = url.copy();
    eurl.replace(" ", "%20");

    if (_useFetchApi) {

        emscripten_fetch_attr_t attr;
        emscripten_fetch_attr_init(&attr);
        strcpy(attr.requestMethod, method.c_str());


        if (_fetchFlags == 1) {
            attr.attributes = EMSCRIPTEN_FETCH_LOAD_TO_MEMORY | EMSCRIPTEN_FETCH_PERSIST_FILE;
        }
        else {
            attr.attributes = EMSCRIPTEN_FETCH_LOAD_TO_MEMORY;
        }

        //printf("attr:%d, %s\n", attr.attributes, attr.requestMethod);

        attr.onsuccess = downloadSucceeded;
        attr.onerror = downloadFailed;
        attr.onprogress = downloadProgress;
        attr.userData = self;

        char* cheaders[128] = { 0 };
        int i = 0;
        for (auto it = headers.begin(); it != headers.end(); ++it) {
            cheaders[i] = (char*)it->first.c_str();
            cheaders[++i] = (char*)it->second.c_str();
            ++i;
            if (i >= 126) break;
        }
        attr.requestHeaders = (const char* const*)cheaders;

        attr.requestData = content.data();
        attr.requestDataSize = content.size();


        _handle = emscripten_fetch(&attr, eurl.c_str());
    }
    else {
        self->_handle = (void*)emscripten_async_wget2_data(eurl.c_str(), method.c_str(), content.c_str(),
                self, true, async_wget2_onload_func, async_wget2_onerror_func, async_wget2_onprogress_func);
    }
    return true;
}

#endif

