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
    printf '%s' "$1" | tr -d '\r\n' | sed -n "s/.*\"$2\"[[:space:]]*:[[:space:]]*\"\([^\"]*\)\".*/\1/p"
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

echo
echo "Крок 1. Запит без токена"
STATUS=$(curl -s -o /tmp/orchestrate-401.json -w '%{http_code}' "$API/users/me")
if [ "$STATUS" != "401" ]; then
    printf '  [FAIL] API недоступний або повернув неочікуваний статус: очікувався 401, отримано %s\n' "$STATUS"
    if grep -q 'ERR_NGROK_8012' /tmp/orchestrate-401.json; then
        printf '         ngrok не може з’єднатися з upstream. Застосунок слухає порт 8080, а тунель спрямовано на localhost:80.\n'
        printf '         Налаштуйте тунель на порт, доступний з його середовища; у docker-compose.yml порт 8080 не опубліковано на хост.\n'
    fi
    rm -f /tmp/orchestrate-401.json
    exit 1
fi
check "GET /api/v1/users/me без Authorization" 401 "$STATUS"
printf '         тіло: %s\n' "$(cat /tmp/orchestrate-401.json)"

echo
echo "Підготовка: користувачі, дім і запрошення ($BASE_URL)"
check "Реєстрація власника (публічний ендпоінт)" 201 "$(register Owner "$OWNER_EMAIL")"
check "Реєстрація мешканця (публічний ендпоінт)" 201 "$(register Resident "$RESIDENT_EMAIL")"

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

HOUSEHOLD_RESPONSE=$(curl -s -w '\n%{http_code}' -X POST "$API/households" \
    -H "Authorization: Bearer $OWNER_TOKEN" -H 'Content-Type: application/json' \
    -d '{"name":"\u041a\u0432\u0430\u0440\u0442\u0438\u0440\u0430"}')
HOUSEHOLD_STATUS=$(printf '%s\n' "$HOUSEHOLD_RESPONSE" | tail -n 1)
HOUSEHOLD_BODY=$(printf '%s\n' "$HOUSEHOLD_RESPONSE" | sed '$d')
check "POST /api/v1/households створює дім" 201 "$HOUSEHOLD_STATUS"
HOUSEHOLD_ID=$(json_field "$HOUSEHOLD_BODY" id)
if [ -z "$HOUSEHOLD_ID" ]; then
    printf '  [FAIL] Не вдалося отримати ID домогосподарства (HTTP %s); відповідь API: %s\n' \
        "$HOUSEHOLD_STATUS" "$HOUSEHOLD_BODY"
    exit 1
fi
OWNER_TOKEN=$(json_field "$(login "$OWNER_EMAIL")" accessToken)
if [ -z "$OWNER_TOKEN" ]; then
    printf '  [FAIL] Не вдалося оновити токен власника після створення домогосподарства\n'
    exit 1
fi
INVITATION_RESPONSE=$(curl -s -w '\n%{http_code}' -X POST "$API/households/$HOUSEHOLD_ID/invitation-code" \
    -H "Authorization: Bearer $OWNER_TOKEN")
INVITATION_STATUS=$(printf '%s\n' "$INVITATION_RESPONSE" | tail -n 1)
INVITATION_BODY=$(printf '%s\n' "$INVITATION_RESPONSE" | sed '$d')
if [ "$INVITATION_STATUS" != "201" ]; then
    printf '  [FAIL] Створення коду запрошення: очікувався 201, отримано %s\n' "$INVITATION_STATUS"
    printf '         тіло: %s\n' "$(printf '%s' "$INVITATION_BODY" | sed 's/"code":"[^"]*"/"code":"[redacted]"/g')"
    exit 1
fi
CODE=$(json_field "$INVITATION_BODY" code)
if [ -z "$CODE" ]; then
    printf '  [FAIL] Не вдалося отримати код запрошення з відповіді API\n'
    exit 1
fi
case "$CODE" in
    ????????)
        case "$CODE" in
            *[!A-Z2-9]*)
                printf '  [FAIL] API повернув код запрошення у неочікуваному форматі\n'
                exit 1
                ;;
        esac
        ;;
    *)
        printf '  [FAIL] API повернув код запрошення неочікуваної довжини (%s замість 8)\n' "${#CODE}"
        exit 1
        ;;
esac
STATUS=$(curl -s -o /tmp/orchestrate-join.json -w '%{http_code}' -X POST "$API/households/join" \
    -H "Authorization: Bearer $RESIDENT_TOKEN" -H 'Content-Type: application/json' \
    -d "{\"code\":\"$CODE\"}")
check "Мешканець приєднався до дому за кодом" 201 "$STATUS"
if [ "$STATUS" != "201" ]; then
    printf '         тіло: %s\n' "$(sed "s/$CODE/[redacted]/g" /tmp/orchestrate-join.json)"
    rm -f /tmp/orchestrate-join.json
    exit 1
fi

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

rm -f /tmp/orchestrate-401.json /tmp/orchestrate-200.json /tmp/orchestrate-403.json /tmp/orchestrate-join.json

echo
if [ "$FAILED" -eq 0 ]; then
    echo "Усі перевірки пройдено."
else
    echo "Є провалені перевірки."
fi
exit "$FAILED"
