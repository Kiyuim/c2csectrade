#!/bin/bash
# Script to build and run the project with JDK 17
set -euo pipefail

# Find and export the JAVA_HOME for JDK 17
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export PATH=$JAVA_HOME/bin:$PATH

echo "Attempting to use Java 17..."
echo "JAVA_HOME set to: $JAVA_HOME"

echo "Using Java version:"
java -version

echo ""
echo "Building backend with Maven..."
# Use the Maven wrapper if it exists, otherwise use the system's mvn
if [ -f "./mvnw" ]; then
  ./mvnw clean package -DskipTests
else
  mvn clean package -DskipTests
fi

# Build frontend locally if npm is available
if command -v npm >/dev/null 2>&1; then
  echo ""
  echo "Building frontend (Vue) locally to produce dist/..."
  (cd frontend && (npm ci || npm install) && npm run build)
else
  echo "npm not found; skipping local frontend build. If frontend image build expects dist/, run 'npm install && npm run build' under frontend/ first."
fi

echo ""
echo "Starting application stack with Docker Compose..."
docker compose up --build
