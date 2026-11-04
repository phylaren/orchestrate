#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
NAME="${1:-orchestrate-$(date +%Y%m%d)}"

if git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  if [ -n "$(git status --porcelain)" ]; then
    echo "УВАГА: є незакомічені зміни — до архіву потрапить лише закомічений стан (HEAD)." >&2
  fi
  git archive --format=zip --prefix="$NAME/" -o "$NAME.zip" HEAD
else
  zip -r "$NAME.zip" . -x 'build/*' '*/build/*' '.gradle/*' '.git/*' '.idea/*' '.env' 'logs/*' '*.zip'
fi

if unzip -l "$NAME.zip" | grep -E '/(build|\.gradle)/|/\.env$' >/dev/null; then
  echo "ПОМИЛКА: в архіві є build/.gradle/.env" >&2
  exit 1
fi
echo "Створено $NAME.zip ($(du -h "$NAME.zip" | cut -f1))"
