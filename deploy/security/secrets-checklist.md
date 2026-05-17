# Secret Rotation Checklist

This project keeps production secrets out of Git. Generate per-environment values, put them in the deployment secret manager, and never paste secret values into logs, tickets, screenshots, or Markdown files.

## Required Before Production

- Generate a fresh `JWT_SECRET` with at least 32 random bytes.
- Rotate `MYSQL_ROOT_PASSWORD` and `MYSQL_APP_PASSWORD`.
- Rotate `RABBITMQ_DEFAULT_PASS`.
- Enable and rotate `REDIS_PASSWORD`.
- Rotate `NACOS_PASSWORD`, `NACOS_AUTH_IDENTITY_KEY`, `NACOS_AUTH_IDENTITY_VALUE`, and `NACOS_AUTH_TOKEN`.
- Keep `AUTH_COOKIE_SECURE=true` in HTTPS environments.
- Store production values in a secret manager or CI/CD protected variables, not in `.env`.
- Restart affected services after every rotation and verify login, token refresh, message publishing, and Nacos config loading.

## Local Generation

Run from the repository root:

```powershell
powershell -ExecutionPolicy Bypass -File deploy/scripts/generate-secrets.ps1 -OutputPath .env.production.local
```

To generate the explicitly named example file requested by the production checklist:

```powershell
powershell -ExecutionPolicy Bypass -File deploy/scripts/generate-secrets.ps1 -OutputPath .env.production.example
```

Both outputs are ignored by Git. Treat them as real secrets if generated.

## Rotation Verification

- Confirm no service logs print secret values.
- Confirm `docker compose --env-file .env.production.local config --quiet` passes.
- Confirm Gateway, login, token refresh, RabbitMQ consumers, Nacos config loading, and Prometheus scrape targets still work.
- Keep a dated record of when each production secret was rotated and who approved it.
