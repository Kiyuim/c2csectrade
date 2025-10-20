#!/usr/bin/env bash
set -euo pipefail

# Script to try multiple Lombok versions using Docker (JDK 23) to find one compatible with the project.
# Usage: ./scripts/try-lombok-upgrades.sh

POM="pom.xml"
BACKUP="${POM}.bak"
cp "$POM" "$BACKUP"

VERSIONS=(
  "1.18.36"
  "1.18.38"
  "1.18.40"
  "1.18.42"
  "1.18.44"
  "1.18.46"
  "1.18.48"
  "1.18.50"
  "1.18.52"
)

DOCKER_IMAGE="maven:3.9.11-jdk-23"

echo "Using Docker image: $DOCKER_IMAGE"

tmpdir=$(mktemp -d)
cleanup(){ rm -rf "$tmpdir"; }
trap cleanup EXIT

success=""
for v in "${VERSIONS[@]}"; do
  echo
  echo "============================================="
  echo "Trying lombok version: $v"
  echo "Updating $POM..."

  # Update the property in the pom (safe perl replacement)
  perl -0777 -pe "s|(<lombok.version>).*?(</lombok.version>)|\1$v\2|s" "$BACKUP" > "$tmpdir/pom.tmp"
  mv "$tmpdir/pom.tmp" "$POM"

  echo "Running Maven build inside Docker (JDK 23)..."
  # Run Maven inside Docker so javac=23 is used (avoids host javac mismatch)
  if docker run --rm -v "$PWD":/workspace -w /workspace $DOCKER_IMAGE mvn -U -DskipTests package > "build-lombok-$v.log" 2>&1; then
    echo "Build succeeded with lombok $v"
    success="$v"
    break
  else
    echo "Build failed with lombok $v (log: build-lombok-$v.log)"
  fi
done

if [ -n "$success" ]; then
  echo "SUCCESS: Project builds with lombok $success"
  echo "pom.xml has been updated to use that version."
  exit 0
else
  echo "No tested Lombok version produced a successful build. Restoring original pom.xml"
  mv "$BACKUP" "$POM"
  exit 1
fi
