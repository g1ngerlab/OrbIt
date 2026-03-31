#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if [[ $# -ge 1 && -n "${1:-}" ]]; then
  export ORBIT_SERVER_HOST="$1"
fi

if [[ $# -ge 2 && -n "${2:-}" ]]; then
  export ORBIT_SERVER_PORT="$2"
fi

cd "$REPO_ROOT"
./gradlew lwjgl3:run


