# Docker 单机 VPS 部署说明

## 部署目标

- 单机 Linux VPS 上使用 Docker Compose 部署前端、网关、5 个微服务和中间件。
- 真实数据库、Redis、RabbitMQ、Nacos 账号密码不写入仓库。
- Java 服务统一从 Nacos Config 读取生产配置。

## 上线前准备

1. 安装 Docker Engine 与 Docker Compose Plugin。
2. 复制根目录 `.env.example` 为 `.env`，填写真实密码。
3. 按需修改 `deploy/mysql/init/01-blog-cloud.sql` 中的初始化管理员信息。
4. 启动基础栈：

```bash
docker compose up -d mysql redis rabbitmq nacos
```

## Nacos 配置导入

登录 `http://<你的服务器IP>:${NACOS_PUBLIC_PORT}`，按 `.env` 中的账号密码登录。

在分组 `BLOG_CLOUD` 下导入这些 Data ID：

- `common.yml`
- `user-service.yml`
- `article-service.yml`
- `comment-service.yml`
- `notify-service.yml`
- `blog-gateway.yml`

对应模板文件在 `deploy/nacos/`。

建议做法：

- `common.yml` 写共享的 MySQL、Redis、RabbitMQ、日志、监控配置。
- 每个服务的 `*.yml` 写端口、Swagger、分页和业务特有配置。
- 如果你希望直接在 Nacos 中写死生产密码，也可以覆盖模板里的 `${...}` 占位符。

## 全量启动

```bash
docker compose up -d --build
```

默认对外暴露：

- 前端：`80`
- 网关：`18080`
- Nacos：`8848`
- RabbitMQ 管理台：`15672`

## 初始化账号

数据库初始化脚本会创建：

- 管理员用户名：`admin`
- 默认密码：`032581`

首次上线后建议立即修改该密码。

## 数据与持久化

Compose 已配置以下持久化卷：

- `mysql-data`
- `redis-data`
- `rabbitmq-data`
- `nacos-data`
- `nacos-logs`
- `avatar-data`

头像文件会保存在 `avatar-data` 卷中，不会因容器重建丢失。

## 发布与回滚

更新代码后执行：

```bash
docker compose up -d --build
```

如需只重建某个服务：

```bash
docker compose up -d --build blog-gateway
```

## 上线后检查

可使用现有脚本 `deploy/post-deploy-check.sh` 做基础探活：

```bash
bash deploy/post-deploy-check.sh http://127.0.0.1:18080
```

## 安全建议

- 不要把根目录 `.env` 上传到公开仓库。
- 生产环境建议关闭不必要的中间件公网暴露，至少限制 MySQL、Redis 仅内网访问。
- Nacos、RabbitMQ 管理台建议配合安全组或反向代理白名单访问。
