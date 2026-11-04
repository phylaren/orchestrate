#!/bin/sh
set -eu
cd "$(dirname "$0")/.."

[ -f .env ] || { echo "Немає .env: виконайте cp .env.example .env і заповніть значення." >&2; exit 1; }

env_value() { grep -E "^$1=" .env | tail -n 1 | cut -d= -f2-; }

DOMAIN=$(env_value DOMAIN)
EMAIL=$(env_value LETSENCRYPT_EMAIL)
STAGING=$(env_value LETSENCRYPT_STAGING)
CORS=$(env_value ORCHESTRATE_SECURITY_CORS_ALLOWED_ORIGINS)

[ -n "$DOMAIN" ] && [ "$DOMAIN" != "orchestrate.example.com" ] || { echo "Задайте реальний DOMAIN у .env." >&2; exit 1; }
[ -n "$EMAIL" ] || { echo "Задайте LETSENCRYPT_EMAIL у .env." >&2; exit 1; }

STAGING_ARG=""
[ "${STAGING:-0}" = "1" ] && STAGING_ARG="--staging"

case "$CORS" in
  *"https://$DOMAIN"*) ;;
  *) echo "УВАГА: ORCHESTRATE_SECURITY_CORS_ALLOWED_ORIGINS не містить https://$DOMAIN — оновіть CORS під публічну адресу." >&2 ;;
esac

echo "1/4 Запускаю застосунок і базу даних..."
docker compose up -d --build app

echo "2/4 Запускаю nginx у тимчасовому режимі bootstrap (лише порт 80 для перевірки домену)..."
NGINX_MODE=bootstrap docker compose up -d --force-recreate nginx

echo "3/4 Отримую сертифікат для $DOMAIN..."
docker compose run --rm --entrypoint certbot certbot certonly \
  --webroot -w /var/www/certbot -d "$DOMAIN" \
  --email "$EMAIL" --agree-tos --no-eff-email --non-interactive $STAGING_ARG

echo "4/4 Перемикаю nginx на HTTPS і запускаю автоматичне поновлення..."
NGINX_MODE=tls docker compose up -d --force-recreate nginx
docker compose --profile tls up -d certbot

echo "Готово: https://$DOMAIN"
[ -n "$STAGING_ARG" ] && echo "Видано тестовий (staging) сертифікат: браузер попередить про недовірений ланцюжок. Для реального поставте LETSENCRYPT_STAGING=0 і повторіть."
