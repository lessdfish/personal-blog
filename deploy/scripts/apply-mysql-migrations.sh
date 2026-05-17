#!/bin/bash

set -euo pipefail

MYSQL_CONTAINER="${MYSQL_CONTAINER:-blog-mysql}"
MIGRATION_DIR="${1:-deploy/mysql/migrations}"

if [ ! -d "$MIGRATION_DIR" ]; then
  echo "[migration] directory not found: $MIGRATION_DIR"
  exit 1
fi

found=0
for file in "$MIGRATION_DIR"/*.sql; do
  if [ ! -e "$file" ]; then
    continue
  fi
  found=1
  echo "[migration] applying $(basename "$file")"
  docker exec -i "$MYSQL_CONTAINER" sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' < "$file"
done

if [ "$found" -eq 0 ]; then
  echo "[migration] no sql files found in $MIGRATION_DIR"
fi
