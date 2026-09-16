e2e_clip_body() {
  local raw="$1"
  E2E_CLIP_BODY_IN="$raw" python3 -c '
import json, os, re
raw = os.environ.get("E2E_CLIP_BODY_IN") or ""
limit = int(os.environ.get("E2E_LOG_BODY_LIMIT") or "1200")
secret = re.compile(r"(?i)(accessToken|refreshToken|token|password|authorization|api[_-]?key)")
def redact(obj):
    if isinstance(obj, dict):
        out = {}
        for k, v in obj.items():
            if secret.search(str(k)):
                out[k] = "<redacted>"
            else:
                out[k] = redact(v)
        return out
    if isinstance(obj, list):
        if len(obj) > 8:
            return [redact(x) for x in obj[:8]] + ["<%d more>" % (len(obj) - 8)]
        return [redact(x) for x in obj]
    if isinstance(obj, str) and len(obj) > 160 and secret.search(obj):
        return "<redacted>"
    return obj
text = raw.strip()
try:
    text = json.dumps(redact(json.loads(text)), ensure_ascii=False, separators=(",", ":"))
except Exception:
    text = secret.sub(r"\1:<redacted>", text)
    text = re.sub(r"(?i)(Bearer\s+)[A-Za-z0-9._\-]+", r"\1<redacted>", text)
if len(text) > limit:
    text = text[:limit] + "...<clipped>"
print(text)
'
}

e2e_step() {
  echo "== $* ==" >&2
}

e2e_doing() {
  echo "DOING: $*" >&2
}

e2e_retry() {
  echo "RETRY: $*" >&2
}

e2e_http_fail() {
  echo "HTTP_FAIL: $*" >&2
}

e2e_http() {
  local method="$1"
  local url="$2"
  shift 2
  echo "DOING: HTTP $method $url" >&2
  echo "URL: $method $url" >&2
  local body=""
  local code="000"
  local curl_ec=0
  local resp=""
  local had_e=0
  case "$-" in *e*) had_e=1 ;; esac
  set +e
  resp="$(curl -sS -X "$method" -w $'\n%{http_code}' "$@" "$url" 2>&1)"
  curl_ec=$?
  if [[ "$had_e" -eq 1 ]]; then set -e; fi
  if [[ "$curl_ec" -ne 0 ]]; then
    echo "HTTP: 000" >&2
    echo "BODY: $(e2e_clip_body "$resp")" >&2
    e2e_http_fail "$method $url curl_exit=$curl_ec"
    printf '%s' "$resp"
    return "$curl_ec"
  fi
  code="$(printf '%s' "$resp" | tail -n 1)"
  if [[ "$resp" == *$'\n'* ]]; then
    body="$(printf '%s' "$resp" | sed '$d')"
  else
    body=""
  fi
  echo "HTTP: $code" >&2
  echo "BODY: $(e2e_clip_body "$body")" >&2
  if [[ ! "$code" =~ ^[23][0-9][0-9]$ ]]; then
    e2e_http_fail "$method $url code=$code"
  fi
  printf '%s' "$body"
  return 0
}

e2e_poll_tr069() {
  local base="$1"
  local token="$2"
  local sub_id="$3"
  local attempts="${4:-12}"
  local sleep_s="${5:-5}"
  local i
  local body=""
  local status=""
  e2e_step "poll TR-069 COMPLETE subscription=$sub_id"
  for i in $(seq 1 "$attempts"); do
    e2e_retry "TR-069 poll $i/$attempts GET $base/subscription/$sub_id/registration-progress"
    local had_e=0
    case "$-" in *e*) had_e=1 ;; esac
    set +e
    body="$(e2e_http GET "$base/subscription/$sub_id/registration-progress" \
      -H "Authorization: Bearer $token")"
    if [[ "$had_e" -eq 1 ]]; then set -e; fi
    status="$(E2E_TR069_BODY="$body" python3 -c '
import json, os
raw = os.environ.get("E2E_TR069_BODY") or ""
try:
    data = json.loads(raw)
except Exception:
    print("")
    raise SystemExit
payload = data.get("data") if isinstance(data, dict) and "data" in data else data
if not isinstance(payload, dict):
    payload = {}
print(payload.get("tr069ProvisionStatus") or payload.get("cpeProvisionStatus") or "")
')"
    echo "TR069: subscription=$sub_id status=${status:-?} attempt=$i/$attempts" >&2
    if [[ "$status" == "COMPLETE" ]]; then
      echo "TR069: COMPLETE" >&2
      return 0
    fi
    if [[ "$status" == "FAILED" || "$status" == "MANUAL_REQUIRED" ]]; then
      e2e_http_fail "TR-069 status=$status subscription=$sub_id"
      return 1
    fi
    sleep "$sleep_s"
  done
  e2e_http_fail "TR-069 poll timeout last=${status:-?} subscription=$sub_id"
  return 1
}
