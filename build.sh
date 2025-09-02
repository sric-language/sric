set -e

cd compiler
sh build.sh
cd ..

bin/sric ./library/sric/module.scm -fmake
bin/sric ./library/test/module.scm -fmake

bin/sric ./library/jsonc/module.scm
bin/sric library/serial/module.scm -fmake
bin/sric library/testSerial/module.scm -fmake

