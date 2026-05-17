#!/bin/bash

set -euo pipefail

BASE_URL="${1:-http://127.0.0.1:18080}"

echo "[check] gateway health"
for i in $(seq 1 60); do
  if curl --noproxy '*' -fsS "$BASE_URL/actuator/health" >/dev/null; then
    break
  fi
  echo "[check] waiting for gateway health... $i"
  sleep 5
done

curl --noproxy '*' -fsS "$BASE_URL/actuator/health" >/dev/null

echo "[check] article page"
curl --noproxy '*' -fsS "$BASE_URL/api/article/page/normal?pageNum=1&pageSize=5" >/dev/null

echo "[check] boards"
curl --noproxy '*' -fsS "$BASE_URL/api/article/board/list" >/dev/null

echo "[check] notify protected endpoint should reject without token"
HTTP_CODE=$(curl --noproxy '*' -s -o /tmp/blog_notify_check.out -w "%{http_code}" "$BASE_URL/api/notify/unread/count")
if [ "$HTTP_CODE" != "200" ]; then
  echo "[check] unexpected http code: $HTTP_CODE"
  exit 1
fi
grep -q '"code":2004\|"code":2005\|"code":401' /tmp/blog_notify_check.out

echo "[check] post-deploy checks passed"
