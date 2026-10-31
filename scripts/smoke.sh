#!/usr/bin/env bash
# Post-deploy smoke test for a running StudyLoop (single-service deploy: SPA + API on one origin).
# Usage: scripts/smoke.sh https://studyloop-bi2z.onrender.com [expected-git-sha]
# Waits for the free-tier instance to wake (and, if a sha is given, for that commit to be live), then checks the
# public pages, API docs, auth guard, demo login, duplicate radar, feed, Pulse and refresh-token rotation.
# Read-only for demo content: it logs in as the demo learner but never posts, answers or accepts.
set -uo pipefail

BASE="${1:?usage: smoke.sh BASE_URL [EXPECTED_SHA]}"
BASE="${BASE%/}"
EXPECT="${2:-}"
JAR="$(mktemp)"
FAILS=0
trap 'rm -f "$JAR"' EXIT

pass() { printf '  PASS  %s\n' "$1"; }
fail() { printf '  FAIL  %s\n' "$1"; FAILS=$((FAILS + 1)); }
json() { python3 -c "import sys, json; d = json.load(sys.stdin); print($1)"; }

echo "Waiting for $BASE/api/health${EXPECT:+ to report commit $EXPECT} ..."
deadline=$((SECONDS + 900))
while :; do
  body="$(curl -s --max-time 100 "$BASE/api/health" || true)"
  status="$(printf '%s' "$body" | json 'd.get("status")' 2>/dev/null || true)"
  version="$(printf '%s' "$body" | json 'd.get("version")' 2>/dev/null || true)"
  if [ "$status" = "UP" ] && { [ -z "$EXPECT" ] || [ "$version" = "$EXPECT" ]; }; then
    echo "  health: $body"
    break
  fi
  if [ "$SECONDS" -ge "$deadline" ]; then
    echo "  health never became ready (last: ${body:-no response})"
    exit 1
  fi
  sleep 10
done

echo "Public pages"
code="$(curl -s -o /tmp/sl_landing -w '%{http_code}' "$BASE/")"
[ "$code" = 200 ] && grep -q '<div id="root">' /tmp/sl_landing && pass "landing serves the React app" || fail "landing ($code)"
code="$(curl -s -o /tmp/sl_route -w '%{http_code}' "$BASE/app/doubts/1")"
[ "$code" = 200 ] && grep -q '<div id="root">' /tmp/sl_route && pass "client route falls back to index.html" || fail "SPA fallback ($code)"
code="$(curl -s -o /dev/null -w '%{http_code}' "$BASE/assets/does-not-exist-123.js")"
[ "$code" = 404 ] && pass "missing asset is a 404, not index.html" || fail "missing asset ($code)"

echo "API docs and guards"
code="$(curl -s -L -o /tmp/sl_docs -w '%{http_code}' "$BASE/api/docs")"
[ "$code" = 200 ] && grep -qi 'swagger' /tmp/sl_docs && pass "Swagger UI is public" || fail "Swagger UI ($code)"
code="$(curl -s -o /dev/null -w '%{http_code}' "$BASE/api/docs/openapi.json")"
[ "$code" = 200 ] && pass "OpenAPI document" || fail "OpenAPI document ($code)"
body="$(curl -s -w '\n%{http_code}' "$BASE/api/doubts")"
[ "$(printf '%s' "$body" | tail -n1)" = 401 ] && printf '%s' "$body" | grep -q UNAUTHENTICATED \
  && pass "protected endpoint returns 401 JSON" || fail "auth guard"

echo "Public stats"
stats="$(curl -s "$BASE/api/public/stats")"
seeded="$(printf '%s' "$stats" | json 'd.get("isSeededDemo")' 2>/dev/null || true)"
[ "$seeded" = True ] && pass "public stats (seeded campus): $stats" || fail "public stats: $stats"

echo "Demo learner"
H=(-H 'Content-Type: application/json' -H 'X-Requested-With: StudyLoop')
login="$(curl -s -c "$JAR" -b "$JAR" "${H[@]}" -X POST "$BASE/api/auth/demo" -d '{"persona":"learner"}')"
TOKEN="$(printf '%s' "$login" | json 'd["accessToken"]' 2>/dev/null || true)"
NAME="$(printf '%s' "$login" | json 'd["user"]["name"]' 2>/dev/null || true)"
if [ -n "$TOKEN" ]; then pass "demo login as $NAME"; else fail "demo login: ${login:0:200}"; fi
A=(-H "Authorization: Bearer $TOKEN")

radar="$(curl -s "${H[@]}" "${A[@]}" -X POST "$BASE/api/doubts/similar" \
  -d '{"title":"How do I check if a relation is in BCNF with two candidate keys?","bodyMd":"R(A,B,C,D) with FDs AB->C, C->A, AB->D. Is it in BCNF? How should I decompose it?"}')"
hits="$(printf '%s' "$radar" | json 'len(d["items"])' 2>/dev/null || echo 0)"
top="$(printf '%s' "$radar" | json '"%s (%.0f%%)" % (d["items"][0]["title"], d["items"][0]["similarity"] * 100)' 2>/dev/null || true)"
[ "${hits:-0}" -ge 1 ] && pass "duplicate radar finds $hits solved doubt(s): $top" || fail "duplicate radar: ${radar:0:200}"

feed="$(curl -s "${H[@]}" "${A[@]}" "$BASE/api/doubts?size=5")"
total="$(printf '%s' "$feed" | json 'd["totalItems"]' 2>/dev/null || true)"
[ -n "$total" ] && pass "campus feed ($total doubts)" || fail "feed: ${feed:0:200}"

pulse="$(curl -s "${H[@]}" "${A[@]}" "$BASE/api/pulse?days=14")"
posted="$(printf '%s' "$pulse" | json 'd["totals"]["doubtsPosted"]' 2>/dev/null || true)"
[ -n "$posted" ] && pass "Campus Pulse (14 days: $posted doubts posted)" || fail "pulse: ${pulse:0:200}"

code="$(curl -s -o /dev/null -w '%{http_code}' -c "$JAR" -b "$JAR" "${H[@]}" -X POST "$BASE/api/auth/refresh")"
[ "$code" = 200 ] && pass "refresh token rotation" || fail "refresh ($code)"
code="$(curl -s -o /dev/null -w '%{http_code}' -c "$JAR" -b "$JAR" "${H[@]}" -X POST "$BASE/api/auth/logout")"
[ "$code" = 204 ] && pass "logout" || fail "logout ($code)"

echo
if [ "$FAILS" -eq 0 ]; then echo "All smoke checks passed."; else echo "$FAILS smoke check(s) failed."; exit 1; fi
