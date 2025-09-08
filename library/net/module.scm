name = sricNet
summary = network
outType = lib
version = 1.0
depends = sric 1.0
srcDirs = ./

fmake.incDirs = ./
fmake.srcDirs = ./
fmake.defines = CURL_DISABLE_LDAP,CURL_STATICLIB,BUILDING_LIBCURL
fmake.msvc.depends = sric 1.0, curl 8
fmake.gcc.depends = sric 1.0, curl 8
fmake.emcc.depends = sric 1.0
