set -e

cd compiler
sh build.sh
cd ..

bin/sric ./library/sric/module.scm -fmake -debug
bin/sric ./library/test/module.scm -fmake -debug
bin/sric ./library/testConcurrent/module.scm -fmake -debug

bin/sric ./library/jsonc/module.scm
bin/sric library/serial/module.scm -fmake -debug
bin/sric library/testSerial/module.scm -fmake -debug

fan fmake output/sric.fmake -debug -G
fan fmake output/test.fmake -debug -G
fan fmake output/testSerial.fmake -debug -G
