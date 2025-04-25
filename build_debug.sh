set -e

cd compiler
sh build.sh
cd ..

sric ./library/std/module.scm -fmake -debug
sric ./library/cstd/module.scm -fmake -debug
sric ./library/test/module.scm -fmake -debug

sric library/serial/module.scm -fmake -debug
sric library/testSerial/module.scm -fmake -debug

fan fmake output/test.fmake -debug -G
fan fmake output/testSerial.fmake -debug -G
