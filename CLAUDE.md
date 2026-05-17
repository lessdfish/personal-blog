# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A microservice-based personal blog forum (明向饭庄) with a Spring Boot 3 backend (Java 17) and Vue 3 + TypeScript frontend. Uses Spring Cloud Gateway for unified API entry, Nacos for service discovery/config, MyBatis for database access, Redis for caching, RabbitMQ for async notifications, and Docker Compose for infrastructure.

**Versions**: Spring Boot 3.2.5, Spring Cloud 2023.0.1, Spring Cloud Alibaba 2023.0.1.0, MySQL 8.4, Redis 7, RabbitMQ 3, Nacos v2.3.2, Java 17, Maven 3.9.9

## Build & Run Commands

### Backend (Maven)
- `mvn -pl blog-common install` — Install shared library first (required before building any service)
- `mvn clean install` — Build all modules
- `mvn -B clean verify` — Run tests with JaCoCo coverage checks (CI)
- `mvn -B -DskipTests package` — Build without tests
- `mvn -pl <module> test -Dtest=<TestClass>` — Run a single test class

### Start services (local dev)
```
mvn -pl blog-common install
mvn -pl user-service spring-boot:run
mvn -pl article-service spring-boot:run
mvn -pl comment-service spring-boot:run
mvn -pl notify-service spring-boot:run
mvn -pl blog-gateway spring-boot:run
```
Services default to `server.port: ${SERVER_PORT:0}` (random port when local with Nacos discovery). Gateway defaults to `18080`. An alternative `.env.example1` provides different local port mappings for Redis/RabbitMQ/Nacos to avoid conflicts.

### Infrastructure (Docker Compose)
- `docker compose up -d redis rabbitmq nacos` — Start dependencies only (for local dev)
- `docker compose up -d --build` — Start full stack including all services

### Frontend (blog-web/)
- `npm run dev` — Vite dev server (port 5173, proxies `/api` to gateway at `18080`)
- `npm run build` — TypeScript type-check + Vite build
- `npm run preview` — Preview production build

### Coverage
- `blog-common`: `com.blogcommon.auth` package requires >95% line coverage (enforced by JaCoCo in CI)

## Architecture

### Microservices (5 backend + 1 frontend)

| Service | Port | Responsibility |
|---|---|---|
| `blog-gateway` | 18080 | Unified entry, routing, CORS, JWT auth filter |
| `user-service` | 8081 | Registration, login, profile, roles/permissions, avatar upload |
| `article-service` | 8082 | Article CRUD, hot list, likes, favorites, boards |
| `comment-service` | 8083 | Comments (hierarchical), pagination, rate limiting |
| `notify-service` | 8084 | Notification query, unread count, RabbitMQ consumers |
| `blog-web` | 5173/80 | Vue 3 SPA (Element Plus, Pinia, Vue Router) |

### Key architectural differences between services

- **blog-gateway** uses reactive Redis (`spring-boot-starter-data-redis-reactive`); all other services use blocking `spring-boot-starter-data-redis`
- **user-service** is the only service with Spring Security and MyBatis-Plus (with code generator + Freemarker templates)
- **article-service** and **comment-service** use OpenFeign + LoadBalancer for inter-service calls; they publish to RabbitMQ
- **comment-service** has a compile-time dependency on `article-service` (for shared types)
- **notify-service** is the only RabbitMQ consumer; article-service and comment-service are producers

### Backend module structure (each service follows this pattern)
```
<module>/src/main/java/com/<service>/
  ├── <Service>Application.java       # @SpringBootApplication + @EnableDiscoveryClient
  ├── config/                          # Security, JWT, Web, OpenAPI, RabbitMQ, Feign, ExceptionHandler
  ├── controller/                      # REST controllers
  ├── service/                         # Business logic
  ├── mapper/                          # MyBatis mappers + SqlProviders
  ├── entity/                          # DB entities
  ├── dto/                             # Request DTOs
  ├── vo/                              # Response VOs
  ├── converter/                       # DTO/Entity/VO converters
  ├── client/                          # OpenFeign clients (inter-service calls)
  └── consumer/                        # RabbitMQ consumers (notify-service only)
```

### Common module (blog-common)
Shared across all services — auth (JWT, TokenSession, RequestUserContext), utilities (JwtUtil, RedisLockUtil), generic `Result<T>` wrapper, enums, exceptions, RabbitMQ message POJOs, constants, logging config.

### Authentication Flow
1. Gateway `GatewayAuthFilter` whitelists public paths (login, register, public article endpoints, Swagger)
2. All other requests require `Authorization: Bearer` JWT token
3. Gateway validates JWT, injects `X-User-Id`, `X-User-Role`, `X-Username` headers downstream
4. Each service can use `@RequireRole`, `@RequirePermission`, `@AdminOnly` annotations for fine-grained access control
5. Redis-backed `TokenSessionValidator` manages session lifecycle

### Gateway Routes
| Prefix | Default (Nacos discovery) | Cloud profile (hardcoded) |
|---|---|---|
| `/api/user/**` | `lb://user-service` | `http://127.0.0.1:9011` |
| `/api/article/**` | `lb://article-service` | `http://127.0.0.1:9020` |
| `/api/comment/**` | `lb://comment-service` | `http://127.0.0.1:9003` |
| `/api/notify/**` | `lb://notify-service` | `http://127.0.0.1:9004` |

All routes use `StripPrefix=1`. The `cloud` profile (`application-cloud.yml`) bypasses Nacos discovery with hardcoded URIs — used in production on the VPS.

### Key Infrastructure
- **Nacos**: Service discovery + configuration management (group: `BLOG_CLOUD`). Each service's `bootstrap.yml` imports `optional:nacos:common.yml` + `optional:nacos:<service-name>.yml` from the `BLOG_CLOUD` group. 6 config files in `deploy/nacos/` — `common.yml` (shared MySQL/Redis/RabbitMQ with env-var placeholders) + per-service overrides. Import via `deploy/import-nacos-configs.sh`.
- **Redis**: Hot data caching, rankings, view/like/favorite counters, session management
- **RabbitMQ**: Async notification delivery — article-service and comment-service publish, notify-service consumes
- **MySQL 8.4**: Two databases — `blog_cloud` (prod) and `blog_cloud_test` (test). Init SQL at `deploy/mysql/init/01-blog-cloud.sql`.

### Database (MySQL 8.4, utf8mb4)
Core tables: `tb_user`, `tb_role`, `tb_permission`, `tb_role_permission`, `tb_article`, `tb_article_like`, `tb_article_favorite`, `tb_board`, `tb_comment`, `tb_notify`

### Frontend (blog-web)
- Pinia stores: `auth.ts` (auth state), `notify.ts` (unread notification count)
- Axios http client with interceptors for auth headers and error handling
- Vue Router with navigation guards (auth required / guest only)
- Markdown rendering via Marked + DOMPurify
- In production, nginx serves the SPA and proxies `/api/` to `blog-gateway:18080` (see `blog-web/nginx.conf`)

### Docker
- `docker/backend-service.Dockerfile` — Multi-stage build: Maven 3.9.9 + Temurin 17 build stage, then JRE-only runtime. Parameterized via `SERVICE_MODULE` and `SERVICE_PORT` build args.
- `blog-web/Dockerfile` — Multi-stage: Node 22 Alpine build, then nginx 1.27 Alpine serving the dist.

### CI/CD
- **CI** (`.github/workflows/ci.yml`): Push/PR to main/master — `mvn -B clean verify` (tests + JaCoCo) then `mvn -B -DskipTests package`. Uploads JARs and JaCoCo reports as artifacts.
- **CD** (`.github/workflows/cd.yml`): Manual dispatch — packages JARs, SCPs to Tencent Cloud VPS at `/opt/blog-cloud/releases/{run_number}`, runs `deploy/restart-services.sh` then `deploy/post-deploy-check.sh` (health endpoint, article page, boards, auth rejection check). Requires secrets: `DEPLOY_HOST`, `DEPLOY_USER`, `DEPLOY_SSH_KEY`, `DEPLOY_PORT`.

## 删除操作 — 严格禁止

- **禁止主动执行任何删除操作**，包括但不限于：`rm`、`rm -rf`、`rmdir`、删除文件/文件夹、`git clean` 等
- 即使是临时文件、构建产物、缓存目录也不得自行删除
- **用户主动要求的删除操作，必须二次确认**：明确列出将要删除的目标路径和内容，等待用户再次确认后方可执行
- 任何删除操作不得跳过此流程，无例外

## Environment Config
- `.env` at root — Docker Compose credentials and ports
- `blog-web/.env.local` — Vite proxy target (`VITE_GATEWAY_TARGET=http://127.0.0.1:18080`)
- Each service has `application.yml` (defaults), `application-test.yml` (test DB), `bootstrap.yml` (Nacos bootstrap)
- Nacos configs in `deploy/nacos/` — loaded via Nacos config import or Docker Compose
