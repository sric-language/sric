/*
 * Copyright (c) 2023, chunquedong
 *
 * This file is part of mgpPro project
 * all rights reserved
 *
 */
#ifndef SRIC_httpclientx_hpp_
#define SRIC_httpclientx_hpp_

#include "RawHttpClient.hpp"

namespace sricNet {

class HttpClient : public RawHttpClient {
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
    bool useCache = false;
public:
    static void initCachePath(const char* dir);
    static bool isNetUrl(const char* uri);

    bool send() override;
    void doReceive() override;
};

}
#endif /* httpclientx_hpp */
