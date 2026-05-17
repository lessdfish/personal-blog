# Backup Baseline

Backups are written under `deploy/backup/output/`, which is ignored by Git. Do not move real backup files into tracked folders.

## Metadata-Only Smoke Test

```powershell
powershell -ExecutionPolicy Bypass -File deploy/backup/backup.ps1 -SkipContainerDumps
```

This verifies the script and captures repository-side recovery inputs such as Nacos configs, Prometheus rules, Grafana provisioning, and Compose files.

## Container Backup

Start the stack, then run:

```powershell
powershell -ExecutionPolicy Bypass -File deploy/backup/backup.ps1
```

The script captures:

- MySQL logical dump with `mysqldump`.
- Redis persistence trigger and `LASTSAVE` output.
- RabbitMQ diagnostics status.
- Elasticsearch index list.
- Nacos, Prometheus, Grafana, and Compose configuration snapshots.

For production, store backups in versioned object storage with lifecycle policies, encryption, access audit, and periodic restore drills.
