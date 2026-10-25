@echo off
echo Starting QuickBite Spring Boot Backend...
set "MAVEN_CMD=C:\Users\creat\tools\apache-maven-3.9.9\bin\mvn.cmd"
if exist "%MAVEN_CMD%" (
    call "%MAVEN_CMD%" spring-boot:run
) else (
    mvn spring-boot:run
)
pause
