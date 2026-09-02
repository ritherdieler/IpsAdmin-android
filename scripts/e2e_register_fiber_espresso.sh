#!/usr/bin/env bash
# Orchestrates Espresso FIBER register e2e + §4 hard cleanup (backend runbook).
# Usage (from Android repo root):
#   ./scripts/e2e_register_fiber_espresso.sh
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND="${BACKEND_ROOT:-$(cd "$ROOT/../ispadmin-backend" && pwd)}"
CLEANUP="$BACKEND/scripts/tr069-e2e-hard-cleanup.sh"
MK_PING="$BACKEND/scripts/tr069-e2e-mk-ping.sh"
ADB="${ADB:-$HOME/Library/Android/sdk/platform-tools/adb}"
PACKAGE="${PACKAGE:-com.dscorp.ispadmin}"
E2E_DNI="${E2E_DNI:-$(python3 -c 'import time; print("9"+("%07d"%(time.time()%10000000)))')}"
E2E_USER="${E2E_USER:-dscorp}"
E2E_PASSWORD="${E2E_PASSWORD:-nohacker}"
E2E_PLACE="${E2E_PLACE:-9 de octubre}"
E2E_WIFI_SSID="${E2E_WIFI_SSID:-mimiwifi}"
E2E_WIFI_PASS="${E2E_WIFI_PASS:-MimiWifi24pass}"
GEO_LON="${GEO_LON:--77.4137}"
GEO_LAT="${GEO_LAT:--11.2177}"

cd "$ROOT"

if [[ ! -x "$CLEANUP" && -f "$CLEANUP" ]]; then
  chmod +x "$CLEANUP"
fi
[[ -f "$CLEANUP" ]] || { echo "Missing $CLEANUP" >&2; exit 1; }
if [[ ! -x "$MK_PING" && -f "$MK_PING" ]]; then
  chmod +x "$MK_PING"
fi
[[ -f "$MK_PING" ]] || { echo "Missing $MK_PING" >&2; exit 1; }

DEVICE="${DEVICE:-$($ADB devices | awk '/device$/{print $1; exit}')}"
[[ -n "$DEVICE" ]] || { echo "No adb device" >&2; exit 1; }
echo "DEVICE=$DEVICE PACKAGE=$PACKAGE E2E_DNI=$E2E_DNI"

echo "== pre cleanup (allow empty) =="
"$CLEANUP" --dni "$E2E_DNI" --allow-empty || true

echo "== prepare emulator location =="
$ADB -s "$DEVICE" emu geo fix "$GEO_LON" "$GEO_LAT" 2>/dev/null || \
  $ADB -s "$DEVICE" shell am broadcast -a android.intent.action.SET_MOCK_LOCATION >/dev/null 2>&1 || true

echo "== ensure prodDebug installed =="
./gradlew :presentation:installProdDebug
echo "== clear app data for clean login =="
$ADB -s "$DEVICE" shell pm clear "$PACKAGE" >/dev/null 2>&1 || true

echo "== connectedProdDebugAndroidTest FiberRegisterFirstOnuE2ETest =="
./gradlew :presentation:connectedProdDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.dscorp.ispadmin.presentation.ui.features.subscription.register.FiberRegisterFirstOnuE2ETest \
  -Pandroid.testInstrumentationRunnerArguments.e2e.dni="$E2E_DNI" \
  -Pandroid.testInstrumentationRunnerArguments.e2e.user="$E2E_USER" \
  -Pandroid.testInstrumentationRunnerArguments.e2e.password="$E2E_PASSWORD" \
  -Pandroid.testInstrumentationRunnerArguments.e2e.place="$E2E_PLACE" \
  -Pandroid.testInstrumentationRunnerArguments.e2e.wifiSsid="$E2E_WIFI_SSID" \
  -Pandroid.testInstrumentationRunnerArguments.e2e.wifiPass="$E2E_WIFI_PASS"

TEST_EXIT=$?

PING_EXIT=0
if [[ "$TEST_EXIT" -eq 0 ]]; then
  echo "== MikroTik2 ping to assigned IP (before cleanup) =="
  set +e
  "$MK_PING" --dni "$E2E_DNI"
  PING_EXIT=$?
  set -e
fi

echo "== post cleanup (required) =="
set +e
"$CLEANUP" --dni "$E2E_DNI"
CLEAN_EXIT=$?
set -e

if [[ "$TEST_EXIT" -ne 0 ]]; then
  echo "E2E test failed exit=$TEST_EXIT" >&2
  exit "$TEST_EXIT"
fi
if [[ "$PING_EXIT" -ne 0 ]]; then
  echo "MikroTik ping validation failed exit=$PING_EXIT (cleanup still ran)" >&2
  exit "$PING_EXIT"
fi
if [[ "$CLEAN_EXIT" -ne 0 ]]; then
  echo "Post cleanup failed exit=$CLEAN_EXIT" >&2
  exit "$CLEAN_EXIT"
fi
echo "E2E_FIBER_ESPRESSO_OK dni=$E2E_DNI wifi=${E2E_WIFI_SSID}/${E2E_WIFI_PASS}"
