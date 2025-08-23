name = sricNet
summary = network
outType = lib
version = 1.0
depends = sric 1.0, cstd 1.0
srcDirs = ./

fmake.srcDirs = ./
fmake.defines = CURL_DISABLE_LDAP,CURL_STATICLIB,BUILDING_LIBCURL
fmake.msvc.depends = sric 1.0, cstd 1.0, concurrent 1.0, curl 8.0.1
fmake.gcc.depends = sric 1.0, cstd 1.0, concurrent 1.0, curl 8.0.1
fmake.emcc.depends = sric 1.0, cstd 1.0
