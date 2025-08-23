/*
 * Copyright (c) 2023, chunquedong
 *
 * This file is part of mgpPro project
 * all rights reserved
 *
 */
#ifndef httpclientx_hpp_
#define httpclientx_hpp_

#include "HttpClient.hpp"

namespace sricNet {

class HttpClientX : public HttpClient {
    bool _localFile = false;
public:
    bool _fromCache = false;
public:
    /**
    * local file path if enable cache
    */
    sric::String cacheFile;

    /**
    * enable local file cache
    */
    bool useCache = true;
public:
    static void initCachePath(const char* dir);
    static bool isNetUrl(const char* uri);

    bool send() override;
    void doReceive() override;
};

}
#endif /* httpclientx_hpp */
