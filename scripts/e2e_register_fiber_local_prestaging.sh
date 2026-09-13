#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND="${BACKEND_ROOT:-$(cd "$ROOT/../ispadmin-backend" && pwd)}"
SCRIPT="$BACKEND/scripts/e2e_register_fiber_local_prestaging.sh"
[[ -f "$SCRIPT" ]] || { echo "Missing $SCRIPT" >&2; exit 1; }
exec "$SCRIPT" "$@"
