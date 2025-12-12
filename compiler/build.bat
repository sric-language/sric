@ECHO OFF

IF "%~1"=="mvn" (
  GOTO :mvndo
) ELSE (
  GOTO :nomvn
)

:mvndo
RMDIR /S /Q goldenFile
mvn package
COPY /Y target\sric-1.0-SNAPSHOT.jar ..\bin\sric-1.0.jar
GOTO :EOF

:nomvn
MKDIR temp

javac -g -d temp -sourcepath src\main\java -cp ../bin/gson-2.8.6.jar ^
    src\main\java\sric\compiler\Main.java
jar cvf ..\bin\sric-1.0.jar -C temp .

RD /S /Q temp
