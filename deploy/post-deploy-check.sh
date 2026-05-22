#!/bin/bash

set -euo pipefail

BASE_URL="${1:-http://127.0.0.1:18080}"

wait_for_url() {
  local name="$1"
  local url="$2"

  echo "[check] $name"
  for i in $(seq 1 60); do
    if curl --noproxy '*' -fsS "$url" >/dev/null; then
      return 0
    fi
    echo "[check] waiting for $name... $i"
    sleep 5
  done

  curl --noproxy '*' -fsS "$url" >/dev/null
}

wait_for_url "gateway health" "$BASE_URL/actuator/health"
wait_for_url "article page" "$BASE_URL/api/article/page/normal?pageNum=1&pageSize=5"
wait_for_url "boards" "$BASE_URL/api/article/board/list"

echo "[check] notify protected endpoint should reject without token"
for i in $(seq 1 60); do
  HTTP_CODE=$(curl --noproxy '*' -s -o /tmp/blog_notify_check.out -w "%{http_code}" "$BASE_URL/api/notify/unread/count")
  if [ "$HTTP_CODE" = "200" ] && grep -q '"code":2004\|"code":2005\|"code":401' /tmp/blog_notify_check.out; then
    break
  fi
  echo "[check] waiting for notify protected endpoint... $i"
  sleep 5
done

HTTP_CODE=$(curl --noproxy '*' -s -o /tmp/blog_notify_check.out -w "%{http_code}" "$BASE_URL/api/notify/unread/count")
if [ "$HTTP_CODE" != "200" ]; then
  echo "[check] unexpected http code: $HTTP_CODE"
  exit 1
fi
grep -q '"code":2004\|"code":2005\|"code":401' /tmp/blog_notify_check.out

echo "[check] post-deploy checks passed"
