#!/usr/bin/env bash

set -euo pipefail

NACOS_URL="${NACOS_URL:-http://127.0.0.1:8848}"
NACOS_USERNAME="${NACOS_USERNAME:-nacos}"
NACOS_PASSWORD="${NACOS_PASSWORD:-nacos}"
NACOS_GROUP="${NACOS_CONFIG_GROUP:-BLOG_CLOUD}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_DIR="${SCRIPT_DIR}/nacos"

CONFIG_FILES=(
  "common.yml"
  "user-service.yml"
  "article-service.yml"
  "comment-service.yml"
  "notify-service.yml"
  "blog-gateway.yml"
)

echo "[nacos] login ${NACOS_URL}"
LOGIN_RESPONSE="$(
  curl -fsS -X POST "${NACOS_URL}/nacos/v1/auth/users/login" \
    -d "username=${NACOS_USERNAME}" \
    -d "password=${NACOS_PASSWORD}"
)"

ACCESS_TOKEN="$(printf '%s' "${LOGIN_RESPONSE}" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')"
if [ -z "${ACCESS_TOKEN}" ]; then
  echo "[nacos] failed to parse accessToken from login response"
  echo "${LOGIN_RESPONSE}"
  exit 1
fi

for file in "${CONFIG_FILES[@]}"; do
  path="${CONFIG_DIR}/${file}"
  if [ ! -f "${path}" ]; then
    echo "[nacos] missing config file: ${path}"
    exit 1
  fi

  echo "[nacos] publish ${file} group=${NACOS_GROUP}"
  curl -fsS -X POST "${NACOS_URL}/nacos/v1/cs/configs" \
    --data-urlencode "dataId=${file}" \
    --data-urlencode "group=${NACOS_GROUP}" \
    --data-urlencode "type=YAML" \
    --data-urlencode "content@${path}" \
    --data-urlencode "accessToken=${ACCESS_TOKEN}" >/dev/null
done

echo "[nacos] all configs imported"
