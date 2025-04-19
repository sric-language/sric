
cd compiler
sh build.sh
cd ..

sric ./library/std/module.scm -fmake
sric ./library/cstd/module.scm -fmake
sric ./library/test/module.scm -fmake

sric library/serial/module.scm -fmake
sric library/testSerial/module.scm -fmake

