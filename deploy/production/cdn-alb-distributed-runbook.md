# CDN, Load Balancing, And Distributed Deployment Runbook

## 1. CDN Rules

Use CDN for:

- `blog-web` static assets.
- Public avatars and public article media after moving local uploads to OSS/object storage.
- Public GET responses only when they are truly anonymous and safe to cache.

Do not CDN-cache:

- Any response with `Authorization`.
- Any response with auth cookies.
- User center, notification, favorites, liked/favorited state, admin APIs, token refresh, login, logout, uploads, or writes.

For any API cache rule, set and verify:

- `Cache-Control` from origin.
- `Vary: Authorization, Cookie` where relevant.
- Query string cache key behavior.
- Short TTL and purge workflow for public article list/detail pages.

## 2. WAF And Gateway

WAF is the primary edge protection for managed rules, bot signals, IP reputation, and emergency blocks. Gateway remains the application-level guard:

- Request size and header/path limits.
- CSRF checks for cookie-authenticated writes.
- Interface-level authorization inside services.
- Redis TTL dynamic IP blocklist for short emergency bans.
- Rate limiting keyed by trusted-proxy client IP parsing.

Never use an IP blacklist as the only defense.

## 3. Load Balancer

Place ALB/CLB before Gateway replicas.

Minimum settings:

- Terminate TLS at ALB or CDN plus ALB.
- Forward `X-Forwarded-Proto`, `X-Forwarded-For`, `X-Real-IP`, and `Host`.
- Health check `GET /actuator/health`.
- Remove failed Gateway replicas automatically.
- Keep Gateway stateless so replicas can scale horizontally.

## 4. Service Replicas

Run multiple replicas of:

- `blog-gateway`
- `user-service`
- `article-service`
- `comment-service`
- `notify-service`
- `blog-web`

Do not store uploaded avatars on a single local container volume in production. Move `avatar-data` to OSS/object storage and store only object keys or public URLs in MySQL.

## 5. Data Layer HA

Replace local single-node infrastructure before production:

- MySQL: managed HA or primary/replica with backups and restore drills.
- Redis: managed Redis or Sentinel/Cluster with password, TLS where available, and eviction policy review.
- RabbitMQ: quorum queues or mirrored queues, DLQ dashboards, and replay limits.
- Nacos: three-node cluster with external database.
- Elasticsearch: at least three nodes, snapshot repository, ILM, and reindex drill.
- Prometheus/Grafana: persistent storage, alert contact points, and dashboard backup.

## 6. Deployment Flow

1. Build immutable images.
2. Run unit tests and frontend build.
3. Run `docker compose config` or Kubernetes manifest validation.
4. Deploy to staging.
5. Import Nacos config and secrets from the secret manager.
6. Run smoke tests and k6 smoke.
7. Run DB migration and backup checks.
8. Shift a small percentage of traffic.
9. Watch alerts, logs, p95 latency, 5xx rate, queue backlog, and login success.
10. Roll forward or roll back with the previous image tag.

## 7. Production Problems To Expect

- Hot article pages cause cache stampede if cache TTLs expire together.
- Login and upload endpoints attract brute force and abuse traffic.
- Search can lag behind MySQL if MQ consumers are down.
- DLQ replay can create a second incident if replayed too quickly.
- CDN can leak private data if `Authorization`, `Cookie`, or `Vary` are mishandled.
- Local file uploads disappear or split across replicas without object storage.
- Nacos config drift can make replicas behave differently.
- Redis outage changes token/session behavior, cache hit rate, rate limiting, and blocklist behavior.
- Elasticsearch reindex can overload MySQL if page size and schedule are not controlled.

## 8. Go-Live Checklist

- HTTPS certificate is valid and auto-renewed.
- Cookies are `HttpOnly`, production `Secure=true`, and `SameSite=Lax` or stricter if compatible.
- CSRF strategy is tested for cookie-authenticated writes.
- WAF rules are enabled and have an emergency block playbook.
- Actuator exposes only `health` publicly and Prometheus privately.
- Secrets are generated per environment and not committed.
- Containers have CPU/memory limits and JVM heap aligned.
- Prometheus rules are loaded and notification channels are tested.
- Backup restore drill has been completed.
- ES reindex endpoint is admin-only and tested in staging.
- MQ DLQ overview and requeue are admin-only and replay-limited.
- k6 smoke and baseline are recorded for the release candidate.
