#!/bin/bash
# One-click: build backend & frontend locally, then docker compose up -d
set -euo pipefail
cd "$(dirname "$0")/.."

# Ensure Java 17
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export PATH=$JAVA_HOME/bin:$PATH

echo "[1/4] Building backend (Maven)..."
if [ -f "./mvnw" ]; then
  ./mvnw clean package -DskipTests
else
  mvn clean package -DskipTests
fi

echo "[2/4] Building frontend (npm)..."
if command -v npm >/dev/null 2>&1; then
  (cd frontend && (npm ci || npm install) && npm run build)
else
  echo "npm not found; please install Node.js/npm to build the frontend."
  exit 1
fi

echo "[3/4] Building docker images..."
docker compose build

echo "[4/4] Starting docker services in background..."
docker compose up -d

echo "Done. Check services:"
docker compose ps

echo "Health: curl -sf http://localhost:8080/actuator/health || true"
echo "Frontend: http://localhost"
echo "MinIO console: http://localhost:9001"

