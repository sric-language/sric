@echo OFF
REM sric launcher

SET SRIC_HOME=%~dp0%..
pushd "%SRIC_HOME%"
SET SRIC_HOME=%CD%
popd


java -cp %SRIC_HOME%/bin/gson-2.8.6.jar;%SRIC_HOME%/bin/sric-1.0.jar sric.compiler.Main -home %SRIC_HOME% %*
