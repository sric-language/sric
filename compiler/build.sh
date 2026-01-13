
if [ "$1" = "mvn" ]; then
    rm -rf goldenFile
    mvn package
    cp target/sric-1.0-SNAPSHOT.jar ../bin/sric-1.0.jar

else
    mkdir temp

    javac -g -d temp -sourcepath src/main/java -cp ../bin/gson-2.8.6.jar \
            src/main/java/sric/compiler/Main.java
    jar cvf ../bin/sric-1.0.jar -C temp .

    rm -r temp
fi
