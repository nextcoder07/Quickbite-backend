@echo off
REM QuickBite Maven Wrapper
set "MAVEN_CMD=C:\Users\creat\tools\apache-maven-3.9.9\bin\mvn.cmd"
if exist "%MAVEN_CMD%" (
    call "%MAVEN_CMD%" %*
) else (
    mvn %*
)
