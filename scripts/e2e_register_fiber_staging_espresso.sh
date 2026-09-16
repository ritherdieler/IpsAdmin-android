#!/usr/bin/env bash
# Orchestrates Espresso FIBER register e2e against staging + §4 hard cleanup.
# Usage (from Android repo root):
#   ./scripts/e2e_register_fiber_staging_espresso.sh \
#     --onu-sn ZTEGDC47BFFD --first-name EeeFiber --last-name Prueba \
#     --wifi-ssid 'ztelab' --wifi-pass '11111111'
# 5 GHz SSID is always "<ssid> - 5G". Password is the same on both bands.
# ONU: --onu-sn (default ZTEGDC47BFFD). Name: --first-name / --last-name.
# WAN: --access-mode pppoe|static (aliases PPPOE_DYNAMIC, STATIC_IP, PPPOE_FIXED).
# Default PPPoE.
# After Espresso, --cleanup-mode auto (default) hard-cleans. --cleanup-mode ask prompts [s/N].
# --cleanup-mode skip / --no-cleanup skips. Aliases: --ask-cleanup, --auto-cleanup, --cleanup.
#
# Agents: run with visible console output; keep the turn open with AwaitShell until done.
# Rely on Cursor's background-job completion notification (gigafiber/AGENTS.md).
set -euo pipefail

CLI_WIFI_SSID=""
CLI_WIFI_PASS=""
CLI_ACCESS_MODE=""
CLI_ONU_SN=""
CLI_FIRST_NAME=""
CLI_LAST_NAME=""
CLEANUP_MODE="${CLEANUP_MODE:-auto}"
while [[ $# -gt 0 ]]; do
  case "$1" in
    --onu-sn)
      [[ $# -ge 2 ]] || { echo "--onu-sn requires a value" >&2; exit 2; }
      CLI_ONU_SN="$2"
      shift 2
      ;;
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
    --first-name)
      [[ $# -ge 2 ]] || { echo "--first-name requires a value" >&2; exit 2; }
      CLI_FIRST_NAME="$2"
      shift 2
      ;;
    --last-name)
      [[ $# -ge 2 ]] || { echo "--last-name requires a value" >&2; exit 2; }
      CLI_LAST_NAME="$2"
      shift 2
      ;;
    --access-mode)
      [[ $# -ge 2 ]] || { echo "--access-mode requires pppoe|static" >&2; exit 2; }
      CLI_ACCESS_MODE="$2"
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
source "$ROOT/scripts/e2e_console.sh"
BACKEND="${BACKEND_ROOT:-$(cd "$ROOT/../ispadmin-backend" && pwd)}"
CLEANUP="$BACKEND/scripts/tr069-e2e-hard-cleanup.sh"
ADB="${ADB:-$HOME/Library/Android/sdk/platform-tools/adb}"
PACKAGE="${PACKAGE:-com.dscorp.ispadmin}"
API_BASE="${API_BASE:-https://api.gigafiberperu.cloud/ispadmin-staging}"
E2E_DNI="${E2E_DNI:-$(python3 -c 'import time; print("9"+("%07d"%(time.time()%10000000)))')}"
E2E_USER="${E2E_USER:-dscorp}"
E2E_PASSWORD="${E2E_PASSWORD:-nohacker}"
E2E_PLACE="${E2E_PLACE:-9 de octubre}"
if [[ -n "$CLI_ONU_SN" ]]; then
  E2E_ONU_SN="$CLI_ONU_SN"
fi
E2E_ONU_SN="${E2E_ONU_SN:-ZTEGDC47BFFD}"
if [[ -n "$CLI_ACCESS_MODE" ]]; then
  E2E_ACCESS_MODE="$CLI_ACCESS_MODE"
fi
E2E_ACCESS_MODE="${E2E_ACCESS_MODE:-PPPOE_DYNAMIC}"
case "$(printf '%s' "$E2E_ACCESS_MODE" | tr '[:upper:]' '[:lower:]')" in
  pppoe|pppoe_dynamic) E2E_ACCESS_MODE="PPPOE_DYNAMIC" ;;
  static|static_ip) E2E_ACCESS_MODE="STATIC_IP" ;;
  pppoe_fixed) E2E_ACCESS_MODE="PPPOE_FIXED" ;;
  *)
    echo "--access-mode / E2E_ACCESS_MODE must be pppoe|static" >&2
    exit 2
    ;;
esac
case "$E2E_ONU_SN" in
  VSOL*|56534F4C*)
    _E2E_WIFI_SSID_DEFAULT="lab-vsol-e2e-24"
    _E2E_WIFI_PASS_DEFAULT="LabVsolWifi24!"
    ;;
  *)
    _E2E_WIFI_SSID_DEFAULT="ztelab"
    _E2E_WIFI_PASS_DEFAULT="11111111"
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
if [[ -n "$CLI_FIRST_NAME" ]]; then
  E2E_FIRST_NAME="$CLI_FIRST_NAME"
fi
if [[ -n "$CLI_LAST_NAME" ]]; then
  E2E_LAST_NAME="$CLI_LAST_NAME"
fi
E2E_FIRST_NAME="${E2E_FIRST_NAME:-EeeFiber}"
E2E_LAST_NAME="${E2E_LAST_NAME:-Prueba}"
E2E_NAP_CODE="${E2E_NAP_CODE:-NO-001}"
GEO_LON="${GEO_LON:--77.4107}"
GEO_LAT="${GEO_LAT:--11.2156}"

cd "$ROOT"

if [[ ! -x "$CLEANUP" && -f "$CLEANUP" ]]; then
  chmod +x "$CLEANUP"
fi
[[ -f "$CLEANUP" ]] || { echo "Missing $CLEANUP" >&2; exit 1; }

DEVICE="${DEVICE:-$($ADB devices | awk '/device$/{print $1; exit}')}"
[[ -n "$DEVICE" ]] || { echo "No adb device" >&2; exit 1; }
E2E_PHASE=alta
e2e_hit alta wait "e2e config" \
  sn="$E2E_ONU_SN" \
  accessMode="$E2E_ACCESS_MODE" \
  ssid="$E2E_WIFI_SSID" \
  dni="$E2E_DNI" \
  cleanupMode="$CLEANUP_MODE" \
  device="$DEVICE"

echo "== pre cleanup (allow empty) =="
e2e_doing "hard cleanup env=staging sn=$E2E_ONU_SN dni=$E2E_DNI allow-empty"
set +e
"$CLEANUP" --env staging --sn "$E2E_ONU_SN" --dni "$E2E_DNI" --allow-empty
PRE_CLEAN_EXIT=$?
set -e
echo "pre-cleanup exit=$PRE_CLEAN_EXIT"

echo "== wait for ONU in unconfigured_onus =="
export E2E_ONU_SN
E2E_PHASE=login
e2e_step "login staging"
TOKEN="$(e2e_http POST "$API_BASE/users/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"$E2E_USER\",\"password\":\"$E2E_PASSWORD\"}" \
  | python3 -c 'import json,sys; print(json.load(sys.stdin).get("accessToken") or "")')"
[[ -n "$TOKEN" ]] || { e2e_hit login fail "login staging" user="$E2E_USER" endpoint="POST /users/login"; exit 1; }
e2e_hit login pass "login staging" user="$E2E_USER" endpoint="POST /users/login"

E2E_PHASE=alta
e2e_step "catalog places"
PLACE_COUNT="$(e2e_http GET "$API_BASE/place" -H "Authorization: Bearer $TOKEN" \
  | python3 -c 'import json,sys; data=json.load(sys.stdin); items=data if isinstance(data,list) else []; print(len(items))')"
if [[ "${PLACE_COUNT:-0}" -lt 1 ]]; then
  echo "Staging catalog has no places; apply ispadmin-backend/scripts/sql/staging-e2e-registration-catalog.sql" >&2
  exit 1
fi

e2e_step "catalog plans"
FIBER_PLAN_COUNT="$(e2e_http GET "$API_BASE/plan" -H "Authorization: Bearer $TOKEN" \
  | python3 -c 'import json,sys; data=json.load(sys.stdin); items=data if isinstance(data,list) else []; print(sum(1 for p in items if (p.get("type") or "").upper()=="FIBER"))')"
if [[ "${FIBER_PLAN_COUNT:-0}" -lt 1 ]]; then
  echo "Staging has no active FIBER plan; apply ispadmin-backend/scripts/sql/staging-e2e-registration-catalog.sql" >&2
  exit 1
fi

e2e_step "catalog napbox"
NAP_COUNT="$(e2e_http GET "$API_BASE/napbox" -H "Authorization: Bearer $TOKEN" \
  | python3 -c 'import json,sys; data=json.load(sys.stdin); items=data if isinstance(data,list) else []; print(len(items))')"
if [[ "${NAP_COUNT:-0}" -lt 1 ]]; then
  echo "Staging has no nap_box rows; apply ispadmin-backend/scripts/sql/staging-e2e-registration-catalog.sql" >&2
  exit 1
fi

e2e_step "catalog core routers"
CORE_COUNT="$(e2e_http GET "$API_BASE/networkDevice/coreTypes" -H "Authorization: Bearer $TOKEN" \
  | python3 -c 'import json,sys; data=json.load(sys.stdin); items=data if isinstance(data,list) else []; print(sum(1 for d in items if not d.get("disabled")))' 2>/dev/null || echo 0)"
if [[ "${CORE_COUNT:-0}" -lt 1 ]]; then
  echo "Staging has no active core router (network_device); apply ispadmin-backend/scripts/sql/staging-e2e-registration-catalog.sql" >&2
  exit 1
fi
echo "catalog ok places=$PLACE_COUNT fiber_plans=$FIBER_PLAN_COUNT nap_boxes=$NAP_COUNT core_routers=$CORE_COUNT"

e2e_step "place/findByLocation"
PLACE_HIT="$(e2e_http GET "$API_BASE/place/findByLocation" -G \
  -H "Authorization: Bearer $TOKEN" \
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
    print("Obligatorio: lat/lon DENTRO del polígono. Lab FIBER: lat=-11.2156 lon=-77.4107 (NAP NO-001).", file=sys.stderr)
    print("Nunca intercambiar lat/lon; MySQL POINT es (lon lat). No usar centro de envelope ni camara mapa default.", file=sys.stderr)
    print("Enviado: latitude=%s longitude=%s" % (lat, lon), file=sys.stderr)
    print(raw, file=sys.stderr)
    sys.exit(1)
if wanted and wanted.lower() not in name.lower():
    print("findByLocation resolved %s but E2E_PLACE=%s (lat=%s lon=%s)" % (name, wanted, lat, lon), file=sys.stderr)
    sys.exit(1)
print("findByLocation ok place=%s lat=%s lon=%s" % (name, lat, lon))
' <<<"$PLACE_HIT"

e2e_step "napbox/near"
NEAR_HIT="$(e2e_http GET "$API_BASE/napbox/near" -G \
  -H "Authorization: Bearer $TOKEN" \
  --data-urlencode "latitude=$GEO_LAT" \
  --data-urlencode "longitude=$GEO_LON")"
E2E_NAP_CODE="$E2E_NAP_CODE" GEO_LAT="$GEO_LAT" GEO_LON="$GEO_LON" python3 -c 'import json,sys,os
raw=sys.stdin.read()
wanted=(os.environ.get("E2E_NAP_CODE") or "").strip().upper()
lat=os.environ.get("GEO_LAT","")
lon=os.environ.get("GEO_LON","")
data=json.loads(raw)
items=data if isinstance(data, list) else (data.get("data") or data.get("response") or [])
if not isinstance(items, list) or not items:
    print("napbox/near vacio para lat=%s lon=%s" % (lat, lon), file=sys.stderr)
    print(raw, file=sys.stderr)
    sys.exit(1)
codes=[str((i or {}).get("code") or "").upper() for i in items]
print("napbox/near ok first=%s count=%s" % (codes[0] if codes else "?", len(codes)))
if wanted and wanted not in codes:
    print("GEO no acerca la NAP pedida: E2E_NAP_CODE=%s ausente en /napbox/near." % wanted, file=sys.stderr)
    print("Usar coords de esa nap_box (lab: NO-001 lat=-11.2156 lon=-77.4107).", file=sys.stderr)
    print("place.latitude/longitude (-11.2177/-77.4137) estan en poligono pero near prioriza otras NAP.", file=sys.stderr)
    print("near=%s" % codes[:12], file=sys.stderr)
    sys.exit(1)
if wanted and codes and codes[0] != wanted:
    print("WARN near[0]=%s wanted=%s (sigue si esta en la lista)" % (codes[0], wanted), file=sys.stderr)
' <<<"$NEAR_HIT"

ONU_OK=0
for ONU_TRY in $(seq 1 12); do
  E2E_HTTP_RETRY="$ONU_TRY/12"
  e2e_retry "unconfigured_onus $ONU_TRY/12 sn=$E2E_ONU_SN"
  set +e
  ONU_BODY="$(e2e_http GET "$API_BASE/onu/unconfigured_onus" -H "Authorization: Bearer $TOKEN")"
  ONU_HTTP=$?
  set -e
  if [[ "$ONU_HTTP" -eq 0 ]] && printf '%s' "$ONU_BODY" | python3 -c 'import json,sys,os,re
wanted=re.sub(r"[^A-Z0-9]","",os.environ["E2E_ONU_SN"].upper())
prefixes={"ZTEG":"5A544547","HWTC":"48575443","VSOL":"56534F4C"}
wanted_hex=wanted
for ascii,hexv in prefixes.items():
  if wanted.startswith(ascii):
    wanted_hex=hexv+wanted[len(ascii):]
    break
data=json.load(sys.stdin)
items=data if isinstance(data,list) else (data.get("response") or data.get("data") or [])
def ok(item):
  disp=re.sub(r"[^A-Z0-9]","",str((item or {}).get("sn","")).upper())
  return wanted in disp or disp in wanted or wanted_hex in disp
sys.exit(0 if any(ok(i) for i in items) else 1)'; then
    e2e_hit alta pass "ONU unconfigured" sn="$E2E_ONU_SN" retry="$ONU_TRY/12"
    ONU_OK=1
    break
  fi
  e2e_retry "waiting for unconfigured $E2E_ONU_SN sleep=5"
  sleep 5
done
unset E2E_HTTP_RETRY
if [[ "$ONU_OK" -ne 1 ]]; then
  e2e_hit alta fail "ONU no en unconfigured_onus" sn="$E2E_ONU_SN" retry="12/12"
  exit 1
fi

echo "== prepare emulator location =="
e2e_doing "adb get-state device=$DEVICE"
$ADB -s "$DEVICE" get-state 2>/dev/null | grep -q device || { echo "Emulator not ready before install" >&2; exit 1; }

echo "== ensure stagingDebug + androidTest installed =="
e2e_doing "gradlew :presentation:installStagingDebug :presentation:installStagingDebugAndroidTest"
./gradlew :presentation:installStagingDebug :presentation:installStagingDebugAndroidTest
echo "== clear app data for clean login =="
e2e_doing "adb pm clear $PACKAGE"
$ADB -s "$DEVICE" shell pm clear "$PACKAGE" >/dev/null 2>&1 || true
$ADB -s "$DEVICE" shell pm path "$PACKAGE.test" >/dev/null 2>&1 || \
  ./gradlew :presentation:installStagingDebugAndroidTest

echo "== connectedStagingDebugAndroidTest FiberRegisterFirstOnuE2ETest =="
E2E_PHASE=alta
e2e_step "espresso start FiberRegisterFirstOnuE2ETest accessMode=$E2E_ACCESS_MODE"
e2e_doing "gradlew :presentation:connectedStagingDebugAndroidTest class=FiberRegisterFirstOnuE2ETest"
set +e
./gradlew :presentation:connectedStagingDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.dscorp.ispadmin.presentation.ui.features.subscription.register.FiberRegisterFirstOnuE2ETest \
  -Pandroid.testInstrumentationRunnerArguments.e2e.dni="$E2E_DNI" \
  -Pandroid.testInstrumentationRunnerArguments.e2e.user="$E2E_USER" \
  -Pandroid.testInstrumentationRunnerArguments.e2e.password="$E2E_PASSWORD" \
  -Pandroid.testInstrumentationRunnerArguments.e2e.place="$E2E_PLACE" \
  -Pandroid.testInstrumentationRunnerArguments.e2e.wifiSsid="$E2E_WIFI_SSID" \
  -Pandroid.testInstrumentationRunnerArguments.e2e.wifiPass="$E2E_WIFI_PASS" \
  -Pandroid.testInstrumentationRunnerArguments.e2e.firstName="$E2E_FIRST_NAME" \
  -Pandroid.testInstrumentationRunnerArguments.e2e.lastName="$E2E_LAST_NAME" \
  -Pandroid.testInstrumentationRunnerArguments.e2e.onuSn="$E2E_ONU_SN" \
  -Pandroid.testInstrumentationRunnerArguments.e2e.accessMode="$E2E_ACCESS_MODE" \
  -Pandroid.testInstrumentationRunnerArguments.e2e.napCode="$E2E_NAP_CODE" \
  -Pandroid.testInstrumentationRunnerArguments.e2e.lat="$GEO_LAT" \
  -Pandroid.testInstrumentationRunnerArguments.e2e.lon="$GEO_LON"
TEST_EXIT=$?
set -e
if [[ "$TEST_EXIT" -eq 0 ]]; then
  e2e_hit alta pass "espresso FiberRegisterFirstOnuE2ETest" \
    sn="$E2E_ONU_SN" accessMode="$E2E_ACCESS_MODE" ssid="$E2E_WIFI_SSID"
else
  e2e_hit alta fail "espresso FiberRegisterFirstOnuE2ETest" \
    sn="$E2E_ONU_SN" accessMode="$E2E_ACCESS_MODE" status="$TEST_EXIT"
fi

if [[ "$TEST_EXIT" -eq 0 ]]; then
  echo "== WiFi credentials (before cleanup) =="
  echo "wifi_24 ssid=$E2E_WIFI_SSID password=$E2E_WIFI_PASS"
  echo "wifi_5 ssid=$E2E_WIFI_SSID_5 password=$E2E_WIFI_PASS"
  echo "== service-health collection =="
  E2E_PHASE=acs
  e2e_doing "GET $API_BASE/subscription/all then service-health + TR-069 poll"
  SUBS_BODY="$(e2e_http GET "$API_BASE/subscription/all" --max-time 60 \
    -H "Authorization: Bearer $TOKEN")"
  SUB_META="$(E2E_DNI="$E2E_DNI" python3 -c '
import json, os, sys
dni=os.environ["E2E_DNI"]
items=json.load(sys.stdin)
subs=items if isinstance(items, list) else []
match=None
for s in subs:
    if str(s.get("dni") or "")==dni:
        match=s
        break
if match is None:
    sys.exit(1)
print("%s\t%s\t%s\t%s" % (
    match.get("id") or "",
    match.get("ip") or "",
    match.get("pppoeUsername") or "",
    match.get("accessMode") or "",
))
' <<<"$SUBS_BODY")" || true
  SUB_ID="${SUB_META%%	*}"
  if [[ -n "$SUB_ID" ]]; then
    E2E_SUB_ID="$SUB_ID"
    E2E_SUB_IP="$(printf '%s' "$SUB_META" | cut -f2)"
    E2E_SUB_PPPOE="$(printf '%s' "$SUB_META" | cut -f3)"
    E2E_SUB_ACCESS="$(printf '%s' "$SUB_META" | cut -f4)"
    e2e_hit acs pass "subscription found" \
      subscription="$SUB_ID" \
      sn="$E2E_ONU_SN" \
      accessMode="${E2E_SUB_ACCESS:-$E2E_ACCESS_MODE}" \
      ip="$E2E_SUB_IP" \
      pppoeUsername="$E2E_SUB_PPPOE"
    HEALTH_BODY="$(e2e_http GET "$API_BASE/subscription/$SUB_ID/service-health" --max-time 60 \
      -H "Authorization: Bearer $TOKEN")"
    SUB_ID="$SUB_ID" python3 -c '
import json, os, sys
health=json.load(sys.stdin)
print("service-health id=%s evaluated_at=%s" % (os.environ["SUB_ID"], health.get("evaluated_at")), file=sys.stderr)
' <<<"$HEALTH_BODY"
    set +e
    e2e_poll_tr069 "$API_BASE" "$TOKEN" "$SUB_ID" 12 5
    TR069_EXIT=$?
    set -e
    e2e_hit acs wait "TR-069 poll exit=$TR069_EXIT (non-fatal)" subscription="$SUB_ID"
  else
    e2e_hit acs fail "subscription not found" dni="$E2E_DNI" sn="$E2E_ONU_SN"
  fi
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
  e2e_doing "hard cleanup env=staging sn=$E2E_ONU_SN dni=$E2E_DNI"
  set +e
  "$CLEANUP" --env staging --sn "$E2E_ONU_SN" --dni "$E2E_DNI"
  CLEAN_EXIT=$?
  set -e
  echo "post-cleanup exit=$CLEAN_EXIT"
else
  e2e_doing "post cleanup skipped cleanupMode=$CLEANUP_MODE"
fi

if [[ "$TEST_EXIT" -ne 0 ]]; then
  e2e_summary fail \
    sn="$E2E_ONU_SN" \
    accessMode="$E2E_ACCESS_MODE" \
    subscription="${E2E_SUB_ID:-}" \
    ip="${E2E_SUB_IP:-}" \
    pppoeUsername="${E2E_SUB_PPPOE:-}" \
    ssid="$E2E_WIFI_SSID" \
    status="$TEST_EXIT"
  echo "E2E test failed exit=$TEST_EXIT" >&2
  exit "$TEST_EXIT"
fi
if [[ "$CLEAN_EXIT" -ne 0 ]]; then
  e2e_summary fail \
    sn="$E2E_ONU_SN" \
    accessMode="$E2E_ACCESS_MODE" \
    subscription="${E2E_SUB_ID:-}" \
    ssid="$E2E_WIFI_SSID" \
    status="$CLEAN_EXIT"
  echo "Post cleanup failed exit=$CLEAN_EXIT" >&2
  exit "$CLEAN_EXIT"
fi
e2e_summary pass \
  sn="$E2E_ONU_SN" \
  accessMode="${E2E_SUB_ACCESS:-$E2E_ACCESS_MODE}" \
  subscription="${E2E_SUB_ID:-}" \
  ip="${E2E_SUB_IP:-}" \
  pppoeUsername="${E2E_SUB_PPPOE:-}" \
  ssid="$E2E_WIFI_SSID"
echo "E2E_FIBER_STAGING_ESPRESSO_OK dni=$E2E_DNI sn=$E2E_ONU_SN wifi_24=${E2E_WIFI_SSID}/${E2E_WIFI_PASS} wifi_5=${E2E_WIFI_SSID_5}/${E2E_WIFI_PASS}"
