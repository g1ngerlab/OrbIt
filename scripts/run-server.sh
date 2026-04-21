#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "Starting server..."
echo "Configure server connection in: core/src/main/java/taltech/orbitgame/network/NetworkConfig.java"

cd "$REPO_ROOT"
./gradlew Server:run


