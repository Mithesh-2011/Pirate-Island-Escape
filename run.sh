#!/usr/bin/env bash
set -e
echo "Building Grand Line Escape..."
mvn -q -DskipTests package
echo "Starting Java Backend Server on http://localhost:8080 ..."
java -jar target/game-server.jar
