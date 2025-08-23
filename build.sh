set -e

cd compiler
sh build.sh
cd ..

bin/sric ./library/std/module.scm -fmake
bin/sric ./library/cstd/module.scm -fmake
bin/sric ./library/test/module.scm -fmake

bin/sric ./library/concurrent/module.scm -fmake
bin/sric ./library/jsonc/module.scm
bin/sric library/serial/module.scm -fmake
bin/sric library/testSerial/module.scm -fmake

