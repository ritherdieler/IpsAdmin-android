#!/usr/bin/env bash
# Orchestrates Espresso FIBER register e2e against staging + §4 hard cleanup.
# Usage (from Android repo root):
#   E2E_ONU_SN=ZTEGDC47BFFD ./scripts/e2e_register_fiber_staging_espresso.sh
#
# Agents: run this script in background and end the turn; do not AwaitShell/poll.
# Rely on Cursor's background-job completion notification (gigafiber/AGENTS.md).
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND="${BACKEND_ROOT:-$(cd "$ROOT/../ispadmin-backend" && pwd)}"
CLEANUP="$BACKEND/scripts/tr069-e2e-hard-cleanup.sh"
MK_PING="$BACKEND/scripts/tr069-e2e-mk-ping.sh"
ADB="${ADB:-$HOME/Library/Android/sdk/platform-tools/adb}"
PACKAGE="${PACKAGE:-com.dscorp.ispadmin}"
API_BASE="${API_BASE:-https://api.gigafiberperu.cloud/ispadmin-staging}"
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
    _E2E_WIFI_SSID_DEFAULT="lab-zte-e2e-24"
    _E2E_WIFI_PASS_DEFAULT="LabZteWifi24!"
    ;;
esac
E2E_WIFI_SSID="${E2E_WIFI_SSID:-$_E2E_WIFI_SSID_DEFAULT}"
E2E_WIFI_PASS="${E2E_WIFI_PASS:-$_E2E_WIFI_PASS_DEFAULT}"
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
if [[ ! -x "$MK_PING" && -f "$MK_PING" ]]; then
  chmod +x "$MK_PING"
fi
[[ -f "$MK_PING" ]] || { echo "Missing $MK_PING" >&2; exit 1; }

DEVICE="${DEVICE:-$($ADB devices | awk '/device$/{print $1; exit}')}"
[[ -n "$DEVICE" ]] || { echo "No adb device" >&2; exit 1; }
echo "DEVICE=$DEVICE PACKAGE=$PACKAGE E2E_DNI=$E2E_DNI E2E_ONU_SN=$E2E_ONU_SN E2E_NAP_CODE=$E2E_NAP_CODE"

echo "== pre cleanup (allow empty) =="
"$CLEANUP" --env staging --sn "$E2E_ONU_SN" --dni "$E2E_DNI" --allow-empty || true

echo "== wait for ONU in unconfigured_onus =="
export E2E_ONU_SN
TOKEN="$(curl -sS -X POST "$API_BASE/users/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"$E2E_USER\",\"password\":\"$E2E_PASSWORD\"}" \
  | python3 -c 'import json,sys; print(json.load(sys.stdin).get("accessToken") or "")')"
[[ -n "$TOKEN" ]] || { echo "Staging login failed" >&2; exit 1; }

PLACE_COUNT="$(curl -sS -H "Authorization: Bearer $TOKEN" "$API_BASE/place" \
  | python3 -c 'import json,sys; data=json.load(sys.stdin); items=data if isinstance(data,list) else []; print(len(items))')"
if [[ "${PLACE_COUNT:-0}" -lt 1 ]]; then
  echo "Staging catalog has no places; apply ispadmin-backend/scripts/sql/staging-e2e-registration-catalog.sql" >&2
  exit 1
fi

FIBER_PLAN_COUNT="$(curl -sS -H "Authorization: Bearer $TOKEN" "$API_BASE/plan" \
  | python3 -c 'import json,sys; data=json.load(sys.stdin); items=data if isinstance(data,list) else []; print(sum(1 for p in items if (p.get("type") or "").upper()=="FIBER"))')"
if [[ "${FIBER_PLAN_COUNT:-0}" -lt 1 ]]; then
  echo "Staging has no active FIBER plan; apply ispadmin-backend/scripts/sql/staging-e2e-registration-catalog.sql" >&2
  exit 1
fi

NAP_COUNT="$(curl -sS -H "Authorization: Bearer $TOKEN" "$API_BASE/napbox" \
  | python3 -c 'import json,sys; data=json.load(sys.stdin); items=data if isinstance(data,list) else []; print(len(items))')"
if [[ "${NAP_COUNT:-0}" -lt 1 ]]; then
  echo "Staging has no nap_box rows; apply ispadmin-backend/scripts/sql/staging-e2e-registration-catalog.sql" >&2
  exit 1
fi

CORE_COUNT="$(curl -sS -H "Authorization: Bearer $TOKEN" "$API_BASE/networkDevice/coreTypes" \
  | python3 -c 'import json,sys; data=json.load(sys.stdin); items=data if isinstance(data,list) else []; print(sum(1 for d in items if not d.get("disabled")))' 2>/dev/null || echo 0)"
if [[ "${CORE_COUNT:-0}" -lt 1 ]]; then
  echo "Staging has no active core router (network_device); apply ispadmin-backend/scripts/sql/staging-e2e-registration-catalog.sql" >&2
  exit 1
fi
echo "catalog ok places=$PLACE_COUNT fiber_plans=$FIBER_PLAN_COUNT nap_boxes=$NAP_COUNT core_routers=$CORE_COUNT"

PLACE_HIT="$(curl -sS -G -H "Authorization: Bearer $TOKEN" \
  "$API_BASE/place/findByLocation" \
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

NEAR_HIT="$(curl -sS -G -H "Authorization: Bearer $TOKEN" \
  "$API_BASE/napbox/near" \
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
for _ in $(seq 1 12); do
  if curl -sS -H "Authorization: Bearer $TOKEN" "$API_BASE/onu/unconfigured_onus" \
    | python3 -c 'import json,sys,os,re
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
# FiberRegisterFirstOnuE2ETest pastes lat/lon into the map; do not use `adb emu geo fix`
# (it often kills the emulator console on medium_phone).
$ADB -s "$DEVICE" get-state 2>/dev/null | grep -q device || { echo "Emulator not ready before install" >&2; exit 1; }

echo "== ensure stagingDebug + androidTest installed =="
./gradlew :presentation:installStagingDebug :presentation:installStagingDebugAndroidTest
echo "== clear app data for clean login =="
$ADB -s "$DEVICE" shell pm clear "$PACKAGE" >/dev/null 2>&1 || true
$ADB -s "$DEVICE" shell pm path "$PACKAGE.test" >/dev/null 2>&1 || \
  ./gradlew :presentation:installStagingDebugAndroidTest

echo "== connectedStagingDebugAndroidTest FiberRegisterFirstOnuE2ETest =="
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
  -Pandroid.testInstrumentationRunnerArguments.e2e.napCode="$E2E_NAP_CODE" \
  -Pandroid.testInstrumentationRunnerArguments.e2e.lat="$GEO_LAT" \
  -Pandroid.testInstrumentationRunnerArguments.e2e.lon="$GEO_LON"
TEST_EXIT=$?
set -e

PING_EXIT=0
if [[ "$TEST_EXIT" -eq 0 ]]; then
  E2E_WIFI_SSID_5="${E2E_WIFI_SSID_5:-${E2E_WIFI_SSID} - 5G}"
  echo "== WiFi credentials (before ping/cleanup) =="
  echo "wifi_24 ssid=$E2E_WIFI_SSID password=$E2E_WIFI_PASS"
  echo "wifi_5 ssid=$E2E_WIFI_SSID_5 password=$E2E_WIFI_PASS"
  echo "== MikroTik2 ping to assigned IP (before cleanup) =="
  set +e
  "$MK_PING" --env staging --dni "$E2E_DNI"
  PING_EXIT=$?
  set -e
fi

if [[ "${SKIP_POST_CLEANUP:-0}" == "1" ]]; then
  echo "== post cleanup skipped (SKIP_POST_CLEANUP=1) dni=$E2E_DNI sn=$E2E_ONU_SN =="
  CLEAN_EXIT=0
else
  echo "== post cleanup (required) =="
  set +e
  "$CLEANUP" --env staging --sn "$E2E_ONU_SN" --dni "$E2E_DNI"
  CLEAN_EXIT=$?
  set -e
fi

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
echo "E2E_FIBER_STAGING_ESPRESSO_OK dni=$E2E_DNI sn=$E2E_ONU_SN wifi_24=${E2E_WIFI_SSID}/${E2E_WIFI_PASS} wifi_5=${E2E_WIFI_SSID_5:-${E2E_WIFI_SSID} - 5G}/${E2E_WIFI_PASS}"
