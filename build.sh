

if [ "$1" = "mvn" ]; then
    cd compiler
    rm -rf goldenFile
    mvn package
    cp target/sric-1.0-SNAPSHOT.jar ../bin/sric-1.0.jar
    cd ..
else
    cd compiler
    sh build.sh
    cd ..
fi

bin/sric ./library/std/module.scm
bin/sric ./library/cstd/module.scm
bin/sric ./library/test/module.scm

