@echo off
title Grand Line Escape - Java Backend
echo Building Grand Line Escape...
mvn -q -DskipTests package
if %errorlevel% neq 0 (
    echo Build failed! Verify Java JDK 21+ and Maven are installed.
    pause
    exit /b %errorlevel%
)
echo Starting Java Backend Server on http://localhost:8080 ...
java -jar target\game-server.jar
pause
