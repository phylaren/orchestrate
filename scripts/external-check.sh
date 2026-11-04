#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

: "${BASE_URL:?Задайте BASE_URL, напр. BASE_URL=https://orchestrate.example.com}"
BASE_URL="${BASE_URL%/}"
OUT="${OUT_DIR:-docs/lecture9/evidence}"
mkdir -p "$OUT"

CURL_ARGS=(-sS --max-time 20)
NEWMAN_ARGS=()
if [ "${INSECURE:-0}" = "1" ]; then
  CURL_ARGS+=(-k)
  NEWMAN_ARGS+=(--insecure)
fi

step() { printf '\n=== %s ===\n' "$1"; }

step "1. Заголовки сторінки входу (схема, HSTS, відсутність Server-версії)"
curl "${CURL_ARGS[@]}" -I "$BASE_URL/login" | tee "$OUT/01-login-headers.txt"

HTTP_URL="${BASE_URL/https:\/\//http://}"
if [ "$HTTP_URL" != "$BASE_URL" ]; then
  step "2. HTTP має перенаправляти на HTTPS"
  { curl "${CURL_ARGS[@]}" -I "$HTTP_URL/login" || true; } | tee "$OUT/02-http-redirect.txt"
fi

step "3. scripts/security-check.sh: 401 → вхід → 200 → 403"
BASE_URL="$BASE_URL" sh scripts/security-check.sh 2>&1 | tee "$OUT/03-security-check.txt"

step "4. Newman: основний сценарій"
npx --yes newman run postman/collection.json -e postman/env.public.json \
  --env-var "baseUrl=$BASE_URL" "${NEWMAN_ARGS[@]}" \
  --reporters cli,junit --reporter-junit-export "$OUT/04-newman-collection.xml" \
  2>&1 | tee "$OUT/04-newman-collection.txt"

step "5. Newman: безпека (401/403, INVALID_TOKEN, claims)"
npx --yes newman run postman/security.collection.json -e postman/env.public.json \
  --env-var "baseUrl=$BASE_URL" "${NEWMAN_ARGS[@]}" \
  --reporters cli,junit --reporter-junit-export "$OUT/05-newman-security.xml" \
  2>&1 | tee "$OUT/05-newman-security.txt"

if [ "${SKIP_DOCKER:-0}" != "1" ] && command -v docker >/dev/null 2>&1; then
  step "6. docker compose ps"
  docker compose ps 2>&1 | tee "$OUT/06-docker-compose-ps.txt" || true

  step "7. access-log Tomcat: має бути реальна адреса клієнта, а не 172.28.0.x"
  docker compose exec -T app sh -c \
    'f=$(find /app /tmp -name "access*.log" 2>/dev/null | head -n 1); [ -n "$f" ] && tail -n 15 "$f"' \
    2>&1 | tee "$OUT/07-access-log.txt" || true
fi
