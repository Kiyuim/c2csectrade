#!/bin/sh
set -e

echo "Waiting for dependent services to be available..."

# Wait for MySQL
MYSQL_HOST=${MYSQL_HOST:-mysql-db}
MYSQL_PORT=${MYSQL_PORT:-3306}
until nc -z ${MYSQL_HOST} ${MYSQL_PORT}; do
  echo "Waiting for MySQL at ${MYSQL_HOST}:${MYSQL_PORT}..."
  sleep 2
done
echo "MySQL is up"

# Wait for Redis
REDIS_HOST=${REDIS_HOST:-redis-cache}
REDIS_PORT=${REDIS_PORT:-6379}
until nc -z ${REDIS_HOST} ${REDIS_PORT}; do
  echo "Waiting for Redis at ${REDIS_HOST}:${REDIS_PORT}..."
  sleep 1
done
echo "Redis is up"

# Wait for MinIO health endpoint
MINIO_ENDPOINT=${MINIO_ENDPOINT:-http://minio:9000}
MINIO_HEALTH_URL="${MINIO_ENDPOINT%/}/minio/health/live"

echo "Waiting for MinIO at ${MINIO_HEALTH_URL} ..."
until curl -fs ${MINIO_HEALTH_URL} >/dev/null 2>&1; do
  echo "Waiting for MinIO..."
  sleep 2
done
echo "MinIO is up"

# Start the application
echo "Starting application..."
exec java -jar /app/app.jar

