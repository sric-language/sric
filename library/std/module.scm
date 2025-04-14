name = sric
summary = system std lib
outType = lib
version = 1.0
depends = 
srcDirs = ./
license = Academic Free License 3.0

fmake.srcDirs = ../../runtime/
fmake.incDirs = ../../runtime/
fmake.msvc.extConfigs.cppflags = /std:c++17
fmake.gcc.extConfigs.cppflags = -std=c++17
