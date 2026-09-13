#!/usr/bin/env bash
# Orchestrates Espresso FIBER register e2e + §4 hard cleanup (backend runbook).
# Usage (from Android repo root):
#   ./scripts/e2e_register_fiber_espresso.sh --wifi-ssid 'mimiwifi' --wifi-pass 'MimiWifi24pass'
# 5 GHz SSID is always "<ssid> - 5G". Password is the same on both bands.
# Env alternatives: E2E_WIFI_SSID, E2E_WIFI_PASS (flags win).
# After Espresso, --cleanup-mode auto (default) hard-cleans. --cleanup-mode ask prompts [s/N].
# --cleanup-mode skip / --no-cleanup skips. Aliases: --ask-cleanup, --auto-cleanup, --cleanup.
# Env: CLEANUP_MODE=ask|auto|skip. SKIP_POST_CLEANUP=1 is skip.
#
# Agents: run in background and end the turn; do not AwaitShell/poll (gigafiber/AGENTS.md).
set -euo pipefail

CLI_WIFI_SSID=""
CLI_WIFI_PASS=""
CLEANUP_MODE="${CLEANUP_MODE:-auto}"
while [[ $# -gt 0 ]]; do
  case "$1" in
    --wifi-ssid)
      [[ $# -ge 2 ]] || { echo "--wifi-ssid requires a value" >&2; exit 2; }
      CLI_WIFI_SSID="$2"
      shift 2
      ;;
    --wifi-pass)
      [[ $# -ge 2 ]] || { echo "--wifi-pass requires a value" >&2; exit 2; }
      CLI_WIFI_PASS="$2"
      shift 2
      ;;
    --cleanup-mode)
      [[ $# -ge 2 ]] || { echo "--cleanup-mode requires ask, auto, or skip" >&2; exit 2; }
      case "$2" in
        ask|auto|skip) CLEANUP_MODE="$2" ;;
        *) echo "--cleanup-mode must be ask, auto, or skip" >&2; exit 2 ;;
      esac
      shift 2
      ;;
    --ask-cleanup)
      CLEANUP_MODE=ask
      shift
      ;;
    --cleanup|--auto-cleanup)
      CLEANUP_MODE=auto
      shift
      ;;
    --no-cleanup)
      CLEANUP_MODE=skip
      SKIP_POST_CLEANUP=1
      shift
      ;;
    -h|--help)
      sed -n '1,11p' "$0"
      exit 0
      ;;
    *)
      echo "Unknown arg: $1" >&2
      exit 2
      ;;
  esac
done

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND="${BACKEND_ROOT:-$(cd "$ROOT/../ispadmin-backend" && pwd)}"
CLEANUP="$BACKEND/scripts/tr069-e2e-hard-cleanup.sh"
ADB="${ADB:-$HOME/Library/Android/sdk/platform-tools/adb}"
PACKAGE="${PACKAGE:-com.dscorp.ispadmin}"
E2E_DNI="${E2E_DNI:-$(python3 -c 'import time; print("9"+("%07d"%(time.time()%10000000)))')}"
E2E_USER="${E2E_USER:-dscorp}"
E2E_PASSWORD="${E2E_PASSWORD:-nohacker}"
E2E_PLACE="${E2E_PLACE:-9 de octubre}"
E2E_ONU_SN="${E2E_ONU_SN:-}"
case "$E2E_ONU_SN" in
  VSOL*|56534F4C*)
    _E2E_WIFI_SSID_DEFAULT="lab-vsol-e2e-24"
    _E2E_WIFI_PASS_DEFAULT="LabVsolWifi24!"
    ;;
  *)
    _E2E_WIFI_SSID_DEFAULT="mimiwifi"
    _E2E_WIFI_PASS_DEFAULT="MimiWifi24pass"
    ;;
esac
E2E_WIFI_SSID="${CLI_WIFI_SSID:-${E2E_WIFI_SSID:-$_E2E_WIFI_SSID_DEFAULT}}"
E2E_WIFI_PASS="${CLI_WIFI_PASS:-${E2E_WIFI_PASS:-$_E2E_WIFI_PASS_DEFAULT}}"
if [[ ${#E2E_WIFI_SSID} -lt 1 || ${#E2E_WIFI_SSID} -gt 27 ]]; then
  echo "SSID WiFi must be 1-27 characters (5 GHz adds ' - 5G')" >&2
  exit 2
fi
if [[ ${#E2E_WIFI_PASS} -lt 8 || ${#E2E_WIFI_PASS} -gt 63 ]]; then
  echo "WiFi password must be 8-63 characters (same for 2.4 and 5)" >&2
  exit 2
fi
E2E_WIFI_SSID_5="${E2E_WIFI_SSID} - 5G"
GEO_LON="${GEO_LON:--77.4107}"
GEO_LAT="${GEO_LAT:--11.2156}"

cd "$ROOT"

if [[ ! -x "$CLEANUP" && -f "$CLEANUP" ]]; then
  chmod +x "$CLEANUP"
fi
[[ -f "$CLEANUP" ]] || { echo "Missing $CLEANUP" >&2; exit 1; }

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

if [[ "$TEST_EXIT" -eq 0 ]]; then
  echo "== WiFi credentials (before cleanup) =="
  echo "wifi_24 ssid=$E2E_WIFI_SSID password=$E2E_WIFI_PASS"
  echo "wifi_5 ssid=$E2E_WIFI_SSID_5 password=$E2E_WIFI_PASS"
fi

should_run_post_cleanup() {
  if [[ "${CLEANUP_MODE}" == "skip" || "${SKIP_POST_CLEANUP:-0}" == "1" ]]; then
    echo "== post cleanup skipped (SKIP_POST_CLEANUP=1) dni=$E2E_DNI sn=${E2E_ONU_SN:-} =="
    return 1
  fi
  if [[ "${CLEANUP_MODE}" == "ask" ]]; then
    local reply=""
    echo "dni=$E2E_DNI sn=${E2E_ONU_SN:-}"
    if [[ -r /dev/tty ]]; then
      if ! read -r -p "¿Ejecutar hard cleanup ahora? [s/N] " reply </dev/tty; then
        reply=""
      fi
    else
      echo "== post cleanup skipped (user) dni=$E2E_DNI sn=${E2E_ONU_SN:-} =="
      return 1
    fi
    case "$reply" in
      s|S|y|Y|si|sí|Si|SI) return 0 ;;
      *)
        echo "== post cleanup skipped (user) dni=$E2E_DNI sn=${E2E_ONU_SN:-} =="
        return 1
        ;;
    esac
  fi
  return 0
}

CLEAN_EXIT=0
if should_run_post_cleanup; then
  echo "== post cleanup =="
  set +e
  "$CLEANUP" --dni "$E2E_DNI"
  CLEAN_EXIT=$?
  set -e
fi

if [[ "$TEST_EXIT" -ne 0 ]]; then
  echo "E2E test failed exit=$TEST_EXIT" >&2
  exit "$TEST_EXIT"
fi
if [[ "$CLEAN_EXIT" -ne 0 ]]; then
  echo "Post cleanup failed exit=$CLEAN_EXIT" >&2
  exit "$CLEAN_EXIT"
fi
echo "E2E_FIBER_ESPRESSO_OK dni=$E2E_DNI wifi_24=${E2E_WIFI_SSID}/${E2E_WIFI_PASS} wifi_5=${E2E_WIFI_SSID_5}/${E2E_WIFI_PASS}"
