/*
 * Copyright (c) 2023, chunquedong
 *
 * This file is part of mgpPro project
 * all rights reserved
 *
 */
#ifndef SRIC_rawhttpclient_hpp_
#define SRIC_rawhttpclient_hpp_

#include <string>
#include <map>
#include "sric.h"

#ifndef __EMSCRIPTEN__
    typedef void CURL;
    #include <thread>
#endif

namespace sricNet {

struct HttpResponse {
    /**
    * response headers
    */
    std::map<std::string, std::string> headers;

    /**
    * request URL
    */
    sric::String url;

    /**
    * local file path if enable cache
    */
    sric::String cacheFile;

    /**
    * RawHttpClient id
    */
    uint64_t id = 0;

    /**
    * Http status code: 200
    */
    int statusCode = 0;

    /**
    * Response content
    */
    sric::String result;
};

/**
* HTTP request client
*/
class RawHttpClient {
public:

#ifndef __EMSCRIPTEN__
    CURL* _curl = NULL;
#else
    void* _handle = NULL;
    //bool _isClosed = false;
    bool _useFetchApi = false;
    int _fetchFlags = 0;
#endif
public:

    std::atomic<float> _progress = 0;
    int _error = 0;
    bool _cancel = false;
public:
    /**
    * HTTP request method: GET or POST
    */
    sric::String method = "GET";

    /**
    * request headers
    */
    std::map<std::string, std::string> headers;

    /**
    * POST content
    */
    sric::String content;

    /**
    * request URL
    */
    sric::String url;

    /**
    * lambda callback in main thread
    */
    std::function<void(HttpResponse& res)> onReceive;

    /**
    * user id for this task
    */
    uint64_t id = 0;

public:
    /**
    * respone content
    */
    HttpResponse _response;

public:
    RawHttpClient();
    ~RawHttpClient();

public:
    
    /**
    * get download progress
    * @return 0..1
    */
    float getProgress();

    /**
    * send async request
    */
    virtual bool send();

public:
    void run();
    bool isCanceled() { return _cancel; }
public:
    virtual void doReceive();
public:
    /**
    * Cancel task
    */
    void cancel();
};


}
#endif /* httpclient_hpp */
