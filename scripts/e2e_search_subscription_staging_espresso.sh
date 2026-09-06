#!/usr/bin/env bash
# Search a staging subscription by DNI in the Android finder.
# Usage (after fiber register with SKIP_POST_CLEANUP=1):
#   E2E_DNI=9xxxxxxx ./scripts/e2e_search_subscription_staging_espresso.sh
#
# Agents: run in background and end the turn; do not AwaitShell/poll (gigafiber/AGENTS.md).
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ADB="${ADB:-$HOME/Library/Android/sdk/platform-tools/adb}"
PACKAGE="${PACKAGE:-com.dscorp.ispadmin}"
E2E_USER="${E2E_USER:-dscorp}"
E2E_PASSWORD="${E2E_PASSWORD:-nohacker}"
E2E_DNI="${E2E_DNI:?E2E_DNI is required}"

cd "$ROOT"
DEVICE="${DEVICE:-$($ADB devices | awk '/device$/{print $1; exit}')}"
[[ -n "$DEVICE" ]] || { echo "No adb device" >&2; exit 1; }

./gradlew :presentation:installStagingDebug
./gradlew :presentation:connectedStagingDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.SubscriptionSearchStagingE2ETest \
  -Pandroid.testInstrumentationRunnerArguments.e2e.dni="$E2E_DNI" \
  -Pandroid.testInstrumentationRunnerArguments.e2e.user="$E2E_USER" \
  -Pandroid.testInstrumentationRunnerArguments.e2e.password="$E2E_PASSWORD"
echo "E2E_SEARCH_STAGING_OK dni=$E2E_DNI"
