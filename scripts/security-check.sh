#!/usr/bin/env sh
# Перевірка безпеки API за чотири кроки:
#   1) запит без токена        -> 401 Unauthorized
#   2) вхід                    -> токен доступу (JWT)
#   3) запит з токеном         -> 200 OK
#   4) користувач без прав     -> 403 Forbidden
#
# Застосунок має працювати на BASE_URL (за замовчуванням http://localhost:8080):
#   ./gradlew bootRun
#   sh scripts/security-check.sh
#
# Скрипт сам реєструє двох користувачів з унікальними email, створює дім і додає мешканця
# за кодом запрошення. Потрібен лише curl.

BASE_URL="${BASE_URL:-http://localhost:8080}"
API="$BASE_URL/api/v1"
PASSWORD="correct-horse-battery"
RUN_ID="$(date +%s)"
OWNER_EMAIL="owner.$RUN_ID@orchestrate.test"
RESIDENT_EMAIL="resident.$RUN_ID@orchestrate.test"
FAILED=0

json_field() {
    # json_field '<json>' <field> — значення рядкового поля верхнього рівня
    printf '%s' "$1" | sed -n "s/.*\"$2\":\"\([^\"]*\)\".*/\1/p"
}

check() {
    # check <назва кроку> <очікуваний статус> <фактичний статус>
    if [ "$2" = "$3" ]; then
        printf '  [OK]   %s: %s\n' "$1" "$3"
    else
        printf '  [FAIL] %s: очікувався %s, отримано %s\n' "$1" "$2" "$3"
        FAILED=1
    fi
}

register() {
    curl -s -o /dev/null -w '%{http_code}' -X POST "$API/users" \
        -H 'Content-Type: application/json' \
        -d "{\"displayName\":\"$1\",\"email\":\"$2\",\"password\":\"$PASSWORD\"}"
}

login() {
    curl -s -X POST "$API/auth/login" \
        -H 'Content-Type: application/json' \
        -d "{\"username\":\"$1\",\"password\":\"$PASSWORD\"}"
}

echo "Підготовка: користувачі, дім і запрошення ($BASE_URL)"
check "Реєстрація власника (публічний ендпоінт)" 201 "$(register Owner "$OWNER_EMAIL")"
check "Реєстрація мешканця (публічний ендпоінт)" 201 "$(register Resident "$RESIDENT_EMAIL")"

echo
echo "Крок 1. Запит без токена"
STATUS=$(curl -s -o /tmp/orchestrate-401.json -w '%{http_code}' "$API/users/me")
check "GET /api/v1/users/me без Authorization" 401 "$STATUS"
printf '         тіло: %s\n' "$(cat /tmp/orchestrate-401.json)"

echo
echo "Крок 2. Вхід і отримання токена"
OWNER_TOKEN=$(json_field "$(login "$OWNER_EMAIL")" accessToken)
RESIDENT_TOKEN=$(json_field "$(login "$RESIDENT_EMAIL")" accessToken)
if [ -n "$OWNER_TOKEN" ] && [ -n "$RESIDENT_TOKEN" ]; then
    printf '  [OK]   POST /api/v1/auth/login: токен %s...\n' "$(printf '%s' "$OWNER_TOKEN" | cut -c1-24)"
else
    printf '  [FAIL] POST /api/v1/auth/login: токен не отримано\n'
    exit 1
fi

echo
echo "Крок 3. Запит з токеном"
STATUS=$(curl -s -o /tmp/orchestrate-200.json -w '%{http_code}' "$API/users/me" \
    -H "Authorization: Bearer $OWNER_TOKEN")
check "GET /api/v1/users/me з Bearer-токеном" 200 "$STATUS"
printf '         тіло: %s\n' "$(cat /tmp/orchestrate-200.json)"

HOUSEHOLD_ID=$(json_field "$(curl -s -X POST "$API/households" \
    -H "Authorization: Bearer $OWNER_TOKEN" -H 'Content-Type: application/json' \
    -d '{"name":"Квартира"}')" id)
CODE=$(json_field "$(curl -s -X POST "$API/households/$HOUSEHOLD_ID/invitation-code" \
    -H "Authorization: Bearer $OWNER_TOKEN")" code)
STATUS=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$API/households/join" \
    -H "Authorization: Bearer $RESIDENT_TOKEN" -H 'Content-Type: application/json' \
    -d "{\"code\":\"$CODE\"}")
check "Мешканець приєднався до дому за кодом" 201 "$STATUS"

STATUS=$(curl -s -o /dev/null -w '%{http_code}' "$API/households/$HOUSEHOLD_ID/members/permissions" \
    -H "Authorization: Bearer $OWNER_TOKEN")
check "Власник (MANAGE_MEMBERS) читає зведення прав" 200 "$STATUS"

echo
echo "Крок 4. Користувач без прав"
STATUS=$(curl -s -o /tmp/orchestrate-403.json -w '%{http_code}' \
    "$API/households/$HOUSEHOLD_ID/members/permissions" \
    -H "Authorization: Bearer $RESIDENT_TOKEN")
check "Мешканець без MANAGE_MEMBERS читає зведення прав" 403 "$STATUS"
printf '         тіло: %s\n' "$(cat /tmp/orchestrate-403.json)"

rm -f /tmp/orchestrate-401.json /tmp/orchestrate-200.json /tmp/orchestrate-403.json

echo
if [ "$FAILED" -eq 0 ]; then
    echo "Усі перевірки пройдено."
else
    echo "Є провалені перевірки."
fi
exit "$FAILED"
