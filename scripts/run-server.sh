#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if [[ $# -ge 1 && -n "${1:-}" ]]; then
  export ORBIT_SERVER_PORT="$1"
fi

cd "$REPO_ROOT"
./gradlew Server:run


