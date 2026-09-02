#!/usr/bin/env bash
# Orchestrates Espresso FIBER register e2e against staging + §4 hard cleanup.
# Usage (from Android repo root):
#   E2E_ONU_SN=ZTEGDC47BFFD ./scripts/e2e_register_fiber_staging_espresso.sh
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
E2E_WIFI_SSID="${E2E_WIFI_SSID:-lab-zte-e2e-24}"
E2E_WIFI_PASS="${E2E_WIFI_PASS:-LabZteWifi24!}"
E2E_ONU_SN="${E2E_ONU_SN:-ZTEGDC47BFFD}"
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
E2E_PLACE="$E2E_PLACE" python3 -c 'import json,sys,os
raw=sys.stdin.read()
data=json.loads(raw)
status=data.get("status")
payload=data.get("data") or {}
name=(payload.get("name") if isinstance(payload, dict) else None) or ""
wanted=os.environ.get("E2E_PLACE","")
if status != 200 or not name:
    print("GEO is outside every place.area polygon; apply scripts/sql/staging-e2e-registration-catalog.sql and use place.latitude/longitude", file=sys.stderr)
    print(raw, file=sys.stderr)
    sys.exit(1)
if wanted and wanted.lower() not in name.lower():
    print("findByLocation resolved %s but E2E_PLACE=%s" % (name, wanted), file=sys.stderr)
    sys.exit(1)
print("findByLocation ok place=%s" % name)
' <<<"$PLACE_HIT"

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
$ADB -s "$DEVICE" emu geo fix "$GEO_LON" "$GEO_LAT" 2>/dev/null || \
  $ADB -s "$DEVICE" shell am broadcast -a android.intent.action.SET_MOCK_LOCATION >/dev/null 2>&1 || true

echo "== ensure stagingDebug installed =="
./gradlew :presentation:installStagingDebug
echo "== clear app data for clean login =="
$ADB -s "$DEVICE" shell pm clear "$PACKAGE" >/dev/null 2>&1 || true

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
  -Pandroid.testInstrumentationRunnerArguments.e2e.onuSn="$E2E_ONU_SN" \
  -Pandroid.testInstrumentationRunnerArguments.e2e.napCode="$E2E_NAP_CODE" \
  -Pandroid.testInstrumentationRunnerArguments.e2e.lat="$GEO_LAT" \
  -Pandroid.testInstrumentationRunnerArguments.e2e.lon="$GEO_LON"
TEST_EXIT=$?
set -e

PING_EXIT=0
if [[ "$TEST_EXIT" -eq 0 ]]; then
  echo "== MikroTik2 ping to assigned IP (before cleanup) =="
  set +e
  "$MK_PING" --env staging --dni "$E2E_DNI"
  PING_EXIT=$?
  set -e
fi

echo "== post cleanup (required) =="
set +e
"$CLEANUP" --env staging --sn "$E2E_ONU_SN" --dni "$E2E_DNI"
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
echo "E2E_FIBER_STAGING_ESPRESSO_OK dni=$E2E_DNI sn=$E2E_ONU_SN wifi=${E2E_WIFI_SSID}/${E2E_WIFI_PASS}"
