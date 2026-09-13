#!/usr/bin/env bash
# E2E FIBER on emulator against local Core WAR (:8082) + Gateway WAR (:8080) + ACS VPS.
# App BASE_URL is http://127.0.0.1:8080/ispadmin/ ; adb reverse maps 8080 → Core 8082.
#
# Usage (from Android repo root):
#   ./scripts/e2e_register_fiber_local_espresso.sh --wifi-ssid 'mimiwifi' --wifi-pass 'MimiWifi24pass'
# 5 GHz SSID is always "<ssid> - 5G". Password is the same on both bands.
# Env alternatives: E2E_WIFI_SSID, E2E_WIFI_PASS (flags win).
# After Espresso, --cleanup-mode auto (default) hard-cleans. --cleanup-mode ask prompts [s/N].
# --cleanup-mode skip / --no-cleanup skips. Aliases: --ask-cleanup, --auto-cleanup, --cleanup.
# Env: CLEANUP_MODE=ask|auto|skip. SKIP_POST_CLEANUP=1 is skip.
#
# Agents: run in background and end the turn; do not AwaitShell/poll (gigafiber/AGENTS.md).
# Runbook: ispadmin-backend/.agent-docs/pruebas-local-gateway-acs-lab.md
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
      sed -n '1,12p' "$0"
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
ADB="${ADB:-$HOME/Library/Android/sdk/platform-tools/adb}"
PACKAGE="${PACKAGE:-com.dscorp.ispadmin.dev}"
CORE="${CORE_BASE:-http://127.0.0.1:8082/ispadmin}"
GW="${GATEWAY_BASE:-http://127.0.0.1:8080/ispadmin}"
MYSQL="${MYSQL_BIN:-/opt/homebrew/opt/mysql-client/bin/mysql}"
LOCAL_PROPS="$BACKEND/src/main/resources/application-local.properties"

E2E_DNI="${E2E_DNI:-$(python3 -c 'import time; print("9"+("%07d"%(time.time()%10000000)))')}"
E2E_USER="${E2E_USER:-dscorp}"
E2E_PASSWORD="${E2E_PASSWORD:-nohacker}"
E2E_PLACE="${E2E_PLACE:-9 de octubre}"
E2E_ONU_SN="${E2E_ONU_SN:-ZTEGDC47BFFD}"
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
E2E_NAP_CODE="${E2E_NAP_CODE:-NO-001}"
GEO_LON="${GEO_LON:--77.4107}"
GEO_LAT="${GEO_LAT:--11.2156}"
export E2E_ONU_SN E2E_NAP_CODE E2E_PLACE GEO_LAT GEO_LON

cd "$ROOT"

if [[ -z "${GKEY:-}" && -f "$LOCAL_PROPS" ]]; then
  GKEY="$(grep '^olt.gateway.api-key=' "$LOCAL_PROPS" | cut -d= -f2-)"
fi
[[ -n "${GKEY:-}" ]] || { echo "GKEY / olt.gateway.api-key missing" >&2; exit 1; }

if [[ -z "${MYSQL_PWD:-}" && -f "$LOCAL_PROPS" ]]; then
  MYSQL_PWD="$(grep '^spring.datasource.password=' "$LOCAL_PROPS" | cut -d= -f2-)"
  export MYSQL_PWD
fi

DEVICE="${DEVICE:-$($ADB devices | awk '/device$/{print $1; exit}')}"
[[ -n "$DEVICE" ]] || { echo "No adb device" >&2; exit 1; }
echo "DEVICE=$DEVICE PACKAGE=$PACKAGE E2E_DNI=$E2E_DNI E2E_ONU_SN=$E2E_ONU_SN E2E_NAP_CODE=$E2E_NAP_CODE"

echo "== health Core + Gateway =="
curl -sS --max-time 15 -o /dev/null -w 'core:%{http_code}\n' "$CORE/actuator/health" | grep -q 'core:200' \
  || { echo "Core no responde en $CORE" >&2; exit 1; }
GW_HEALTH="$(curl -sS --max-time 20 -H "X-Olt-Gateway-Key: $GKEY" "$GW/api/olt-gateway/health")"
echo "$GW_HEALTH" | python3 -c 'import sys,json; d=json.load(sys.stdin); assert d.get("oltReachable") is True, d' \
  || { echo "Gateway oltReachable!=true: $GW_HEALTH" >&2; exit 1; }

echo "== adb reverse emulator 8080 -> Core 8082 =="
$ADB -s "$DEVICE" reverse tcp:8080 tcp:8082
$ADB -s "$DEVICE" reverse --list

local_clean_onu() {
  local sn="$1"
  local raw ext
  raw="$(curl -sS --max-time 45 -H "X-Olt-Gateway-Key: $GKEY" "$GW/api/olt-gateway/onu/get_onus_details_by_sn/$sn" || true)"
  ext="$(echo "$raw" | python3 -c '
import sys,json
try:
  d=json.load(sys.stdin)
except Exception:
  print(""); sys.exit(0)
onus=d.get("onus") or d.get("response") or []
if isinstance(onus, dict):
  onus=onus.get("onus") or []
print((onus[0] or {}).get("unique_external_id") or "" if isinstance(onus, list) and onus else "")
')"
  if [[ -n "$ext" ]]; then
    echo "delete Gateway $ext"
    curl -sS --max-time 60 -X POST -H "X-Olt-Gateway-Key: $GKEY" "$GW/api/olt-gateway/onu/delete/$ext" || true
    echo
  fi
  if [[ -n "${MYSQL_PWD:-}" && -x "$MYSQL" ]]; then
    "$MYSQL" -uroot -e "DELETE FROM dev_oltgateway.olt_activation_operation WHERE sn='$sn';" || true
    "$MYSQL" -uroot ispadmin_dev -e "UPDATE subscription SET fiber_onu_sn=NULL WHERE fiber_onu_sn='$sn';" || true
  fi
}

echo "== pre cleanup local ONU (allow empty) =="
local_clean_onu "$E2E_ONU_SN"

echo "== ensure local e2e user + catalog =="
"$BACKEND/scripts/local-e2e-ensure-catalog.sh"

echo "== login Core =="
TOKEN="$(curl -sS --max-time 20 -X POST "$CORE/users/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"$E2E_USER\",\"password\":\"$E2E_PASSWORD\"}" \
  | python3 -c 'import json,sys; print(json.load(sys.stdin).get("accessToken") or "")')"
[[ -n "$TOKEN" ]] || { echo "Core login failed user=$E2E_USER" >&2; exit 1; }

PLACE_HIT="$(curl -sS -G -H "Authorization: Bearer $TOKEN" \
  "$CORE/place/findByLocation" \
  --data-urlencode "latitude=$GEO_LAT" \
  --data-urlencode "longitude=$GEO_LON")"
E2E_PLACE="$E2E_PLACE" GEO_LAT="$GEO_LAT" GEO_LON="$GEO_LON" python3 -c 'import json,sys,os
raw=sys.stdin.read()
data=json.loads(raw)
status=data.get("status")
payload=data.get("data") or {}
name=(payload.get("name") if isinstance(payload, dict) else None) or ""
wanted=os.environ.get("E2E_PLACE","")
lat=os.environ.get("GEO_LAT","")
lon=os.environ.get("GEO_LON","")
if status != 200 or not name:
    print("GEO fuera de todo place.area (findByLocation != 200).", file=sys.stderr)
    print("Lab local FIBER: mismos datos que e2e prod (9 de octubre / NAP NO-001 lat=-11.2156 lon=-77.4107).", file=sys.stderr)
    print("Enviado: latitude=%s longitude=%s" % (lat, lon), file=sys.stderr)
    print(raw, file=sys.stderr)
    sys.exit(1)
if wanted and wanted.lower() not in name.lower():
    print("findByLocation resolved %s but E2E_PLACE=%s (lat=%s lon=%s)" % (name, wanted, lat, lon), file=sys.stderr)
    sys.exit(1)
print("findByLocation ok place=%s lat=%s lon=%s" % (name, lat, lon))
' <<<"$PLACE_HIT"

NEAR_HIT="$(curl -sS -G -H "Authorization: Bearer $TOKEN" \
  "$CORE/napbox/near" \
  --data-urlencode "latitude=$GEO_LAT" \
  --data-urlencode "longitude=$GEO_LON")"
E2E_NAP_CODE="$E2E_NAP_CODE" python3 -c 'import json,sys,os,re
raw=sys.stdin.read()
wanted=re.sub(r"[^A-Z0-9]","",(os.environ.get("E2E_NAP_CODE") or "").upper())
data=json.loads(raw)
items=data if isinstance(data, list) else (data.get("data") or data.get("response") or [])
if not isinstance(items, list) or not items:
    print("napbox/near vacio", file=sys.stderr)
    print(raw, file=sys.stderr)
    sys.exit(1)
codes=[re.sub(r"[^A-Z0-9]","",str((i or {}).get("code") or "").upper()) for i in items]
print("napbox/near ok first=%s count=%s" % ((items[0] or {}).get("code"), len(items)))
if wanted and wanted not in codes:
    print("E2E_NAP_CODE ausente en /napbox/near wanted=%s near=%s" % (wanted, codes[:12]), file=sys.stderr)
    sys.exit(1)
' <<<"$NEAR_HIT"

echo "== wait autofind lab ONU =="
ONU_OK=0
for _ in $(seq 1 18); do
  if curl -sS --max-time 30 -H "X-Olt-Gateway-Key: $GKEY" "$GW/api/olt-gateway/onu/unconfigured_onus" \
    | python3 -c 'import json,sys,os,re
wanted=re.sub(r"[^A-Z0-9]","",os.environ["E2E_ONU_SN"].upper())
wanted_hex="5A544547"+wanted[4:] if wanted.startswith("ZTEG") else wanted
data=json.load(sys.stdin)
items=data.get("response") or data.get("onus") or data.get("data") or []
if isinstance(items, dict):
  items=items.get("onus") or items.get("data") or []
def ok(item):
  disp=re.sub(r"[^A-Z0-9]","",str((item or {}).get("sn","")).upper())
  return wanted in disp or disp in wanted or wanted_hex in disp
sys.exit(0 if any(ok(i) for i in items) else 1)'; then
    echo "ONU $E2E_ONU_SN is unconfigured"
    ONU_OK=1
    break
  fi
  echo "waiting for unconfigured $E2E_ONU_SN ..."
  sleep 5
done
if [[ "$ONU_OK" -ne 1 ]]; then
  echo "ONU lab $E2E_ONU_SN no en unconfigured_onus" >&2
  exit 1
fi

echo "== prepare emulator location =="
$ADB -s "$DEVICE" get-state 2>/dev/null | grep -q device || { echo "Emulator not ready before install" >&2; exit 1; }

echo "== installDevDebug + androidTest =="
./gradlew :presentation:installDevDebug :presentation:installDevDebugAndroidTest
echo "== clear app data for clean login =="
$ADB -s "$DEVICE" shell pm clear "$PACKAGE" >/dev/null 2>&1 || true
$ADB -s "$DEVICE" reverse tcp:8080 tcp:8082

echo "== connectedDevDebugAndroidTest FiberRegisterFirstOnuE2ETest =="
set +e
./gradlew :presentation:connectedDevDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.dscorp.ispadmin.presentation.ui.features.subscription.register.FiberRegisterFirstOnuE2ETest \
  -Pandroid.testInstrumentationRunnerArguments.e2e.dni="$E2E_DNI" \
  -Pandroid.testInstrumentationRunnerArguments.e2e.user="$E2E_USER" \
  -Pandroid.testInstrumentationRunnerArguments.e2e.password="$E2E_PASSWORD" \
  -Pandroid.testInstrumentationRunnerArguments.e2e.place="$E2E_PLACE" \
  -Pandroid.testInstrumentationRunnerArguments.e2e.wifiSsid="$E2E_WIFI_SSID" \
  -Pandroid.testInstrumentationRunnerArguments.e2e.wifiPass="$E2E_WIFI_PASS" \
  -Pandroid.testInstrumentationRunnerArguments.e2e.onuSn="$E2E_ONU_SN" \
  -Pandroid.testInstrumentationRunnerArguments.e2e.napCode="$E2E_NAP_CODE" \
  -Pandroid.testInstrumentationRunnerArguments.e2e.lat="$GEO_LAT" \
  -Pandroid.testInstrumentationRunnerArguments.e2e.lon="$GEO_LON"
TEST_EXIT=$?
set -e

if [[ "$TEST_EXIT" -eq 0 ]]; then
  echo "== WiFi credentials (before cleanup) =="
  echo "wifi_24 ssid=$E2E_WIFI_SSID password=$E2E_WIFI_PASS"
  echo "wifi_5 ssid=$E2E_WIFI_SSID_5 password=$E2E_WIFI_PASS"
fi

should_run_post_cleanup() {
  if [[ "${CLEANUP_MODE}" == "skip" || "${SKIP_POST_CLEANUP:-0}" == "1" ]]; then
    echo "== post cleanup skipped (SKIP_POST_CLEANUP=1) dni=$E2E_DNI sn=$E2E_ONU_SN =="
    return 1
  fi
  if [[ "${CLEANUP_MODE}" == "ask" ]]; then
    local reply=""
    echo "dni=$E2E_DNI sn=$E2E_ONU_SN"
    if [[ -r /dev/tty ]]; then
      if ! read -r -p "¿Ejecutar hard cleanup ahora? [s/N] " reply </dev/tty; then
        reply=""
      fi
    else
      echo "== post cleanup skipped (user) dni=$E2E_DNI sn=$E2E_ONU_SN =="
      return 1
    fi
    case "$reply" in
      s|S|y|Y|si|sí|Si|SI) return 0 ;;
      *)
        echo "== post cleanup skipped (user) dni=$E2E_DNI sn=$E2E_ONU_SN =="
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
  local_clean_onu "$E2E_ONU_SN"
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
echo "E2E_FIBER_LOCAL_ESPRESSO_OK dni=$E2E_DNI sn=$E2E_ONU_SN wifi_24=${E2E_WIFI_SSID}/${E2E_WIFI_PASS} wifi_5=${E2E_WIFI_SSID_5}/${E2E_WIFI_PASS}"
