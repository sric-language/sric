
if [ "$1" = "mvn" ]; then
    rm -rf goldenFile
    mvn package
    cp target/sric-1.0-SNAPSHOT.jar ../bin/sric-1.0.jar

else
    mkdir temp

    find src/main/java -name '*.java' | xargs javac -g -d temp -cp ../bin/gson-2.8.6.jar
    jar cvf ../bin/sric-1.0.jar -C temp .

    rm -r temp
fi
