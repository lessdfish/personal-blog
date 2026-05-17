# Restore Runbook

Use this as the minimum restore checklist. Practice it before production launch.

## 1. Pick A Backup

Choose a timestamped folder under `deploy/backup/output/` or the production backup bucket. Confirm it contains:

- `mysql-blog-cloud.sql`
- `nacos-configs/`
- `prometheus-config/`
- `grafana-provisioning/`
- `rabbitmq-status.txt`
- `elasticsearch-indices.txt`

## 2. Restore MySQL

```powershell
Get-Content deploy/backup/output/<timestamp>/mysql-blog-cloud.sql | docker exec -i blog-mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD"'
```

After restore, verify login, article list, comments, notifications, and admin operations.

## 3. Restore Nacos Config

Re-import the YAML files from the backup folder to Nacos using the existing import script or Nacos console. Restart services after config import so they read the restored values.

## 4. Restore Redis

Redis is a cache/session store in this project. Prefer rebuilding cache from MySQL and forcing users to re-login after major incidents. If production starts storing durable Redis data, enable RDB/AOF backup export and document the exact restore command for that deployment.

## 5. Restore RabbitMQ

RabbitMQ messages are operational state. For production, export definitions and policies through the management API. After restore, inspect retry queues and DLQs before enabling consumers to avoid a replay storm.

## 6. Restore Elasticsearch

MySQL is the source of truth for articles. If Elasticsearch data is missing or stale, restore the cluster first, then run the article reindex operation from phase 12. Do not treat Elasticsearch as the only article backup.

## 7. Verify

- Gateway health is up.
- Prometheus `/ - /ready` equivalent endpoint is ready: `http://localhost:9090/-/ready`.
- Login and token refresh work.
- Article list/detail/search work.
- Comment and notification flows work.
- DLQ counts are reviewed before reopening traffic.
