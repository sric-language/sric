
cd compiler
rm -rf goldenFile
mvn package
cp target/sric-1.0-SNAPSHOT.jar ../bin/sric-1.0.jar
cd ..

bin/sric ./library/std/module.scm


