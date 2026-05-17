# Code Review Report: codex/depth_develop

> 审查时间：2026-05-13
> 审查范围：当前分支相对 main 的所有变更（71 个文件，+1615/-320 行）
> 审查维度：CLAUDE.md 合规性、Bug 扫描、安全审查、架构设计、测试质量

---

## 高风险问题

### 1. [安全] CORS 允许任意来源 + 携带凭证

- **文件**: `deploy/nacos/blog-gateway.yml`
- **描述**: `allowedOriginPatterns: "*"` 与 `allowCredentials: true` 同时使用。任意恶意网站可向 API 发起携带 Cookie/JWT 的跨域请求，构成 CSRF 攻击向量。
- **建议**: 将 `allowedOriginPatterns` 限定为实际前端域名（如 `https://yourdomain.com`），不要使用通配符。

### 2. [安全] .run/*.xml 硬编码数据库密码

- **文件**: `.run/01_UserService.run.xml`、`.run/02_ArticleService.run.xml` 等（6 个文件）
- **描述**: MySQL 密码 `032581`、RabbitMQ 凭据 `guest/guest` 明文写在 IDE 运行配置中，且 `.run/` 目录未加入 `.gitignore`，密码随 Git 提交泄露。
- **建议**: 将 `.run/` 加入 `.gitignore`，密码改为环境变量引用。已泄露的密码需尽快更换。

### 3. [CLAUDE.md] UserMapper 新增 6 个查询全部使用 SELECT *

- **文件**: `user-service/src/main/java/com/userservice/mapper/UserMapper.java`
- **描述**: `selectByNickname`、`selectByEmail`、`selectByPhone` 及其 ExcludeId 变体全部用 `SELECT *`，违反 CLAUDE.md "SQL 语句中不要使用 SELECT *，明确列出所需字段" 规则。
- **建议**: 明确列出所需字段名。

---

## 中风险问题

### 4. [配置] Nacos 默认端口不一致 (8848 vs 8948)

- **文件**: 多个服务的 `application.yml` 和 `bootstrap.yml`
- **描述**: `bootstrap.yml` 默认值 `127.0.0.1:8848`，`application.yml` 默认值 `127.0.0.1:8948`。notify-service 没有 bootstrap.yml 只有 8948。未设置环境变量时各服务连接不同端口。
- **建议**: 统一所有默认值为 `8848`，与 Nacos 标准端口一致。

### 5. [架构] article-service 热路径同步调用 user-service 获取用户名

- **文件**: `article-service/src/main/java/com/articleservice/service/ArticleService.java`（`getUserName` 方法）
- **描述**: 点赞/收藏的热路径上通过 Feign 同步调用 user-service 获取用户名，引入了对 user-service 的同步依赖。网关已在 header 中注入 `X-Username`，无需额外 Feign 调用。
- **建议**: 从 `X-Username` header 获取用户名，或在 notify-service 消费端查询。

### 6. [架构] UserClient 缺少 fallback，存在级联故障风险

- **文件**: `article-service/src/main/java/com/articleservice/client/UserClient.java`
- **描述**: `@FeignClient` 未配置 `fallback` 或 `fallbackFactory`。user-service 不可用时，所有涉及文章点赞/收藏的请求都会因 Feign 调用失败而抛异常。
- **建议**: 添加 `UserClientFallback` 实现，在服务不可用时返回默认值。

### 7. [Bug] 前端 PublishView / ArticleDetailView 无错误处理

- **文件**: `blog-web/src/views/PublishView.vue`、`blog-web/src/views/ArticleDetailView.vue`
- **描述**: `submit()` 直接 `await publishArticle(form)` 无 try-catch，API 失败时用户无任何提示，出现 unhandled promise rejection。`loadDetail()` 只有 `finally` 无 `catch`，文章不存在时页面空白无提示。
- **建议**: 添加 try-catch 并使用 ElMessage 提示错误信息。

### 8. [安全] 密码重置仅凭用户名+手机号，无二次验证

- **文件**: `user-service/src/main/java/com/userservice/service/UserService.java`（`resetPasswordByPhone` 方法）
- **描述**: 密码重置端点仅校验"用户名+手机号"即可重置密码，无短信验证码或邮箱确认。当前需 JWT 认证风险可控，但若路径被加入白名单则任何人都可重置任意账户。
- **建议**: 增加短信验证码二次校验机制。

### 9. [CLAUDE.md] 文章互动通知 action 字段硬编码中文

- **文件**: `article-service/.../ArticleService.java`（`sendArticleInteractionNotify`）、`notify-service/.../NotifyService.java`（`resolveInteractionType`）
- **描述**: `"点赞"` 和 `"收藏"` 作为字符串硬编码在 producer 和 consumer 两侧，新增 action 类型（如"转发"）时容易出错，且中英文混用不符合常量规范。
- **建议**: 定义 `LIKE`、`FAVORITE` 常量放在 `blog-common/MqConstants` 中。

### 10. [测试] 多个新功能无测试覆盖

- **文件**: `ArticleServiceTest`、`NotifyServiceTest`、`UserServiceTest`、`CommentServiceTest`
- **描述**: 以下新增功能无任何单元测试：`syncHeat`（热度全量重算）、`sendArticleInteractionNotify`（MQ 通知）、`handleArticleInteractionNotify`（通知消费）、`resetPasswordByPhone`（密码重置）、`isFieldAvailable`（字段校验）、`DbWriteAuditLogger`（审计日志）。
- **建议**: 为核心业务逻辑补充单元测试，至少覆盖主路径和边界条件。

### 11. [配置] docker-compose 环境变量名不一致

- **文件**: `docker-compose.yml`、`deploy/nacos/common.yml`
- **描述**: docker-compose 传入 `MYSQL_APP_USERNAME`/`MYSQL_APP_PASSWORD`，桥接赋值给 `MYSQL_USERNAME`/`MYSQL_PASSWORD`，common.yml 再读取后者。链条虽能跑通但增加理解和维护成本。
- **建议**: 统一变量命名，直接使用 `MYSQL_USERNAME`/`MYSQL_PASSWORD`。

---

## 低风险问题

### 12. [Bug] ArticleService.getUserName 静默吞掉所有异常

- **文件**: `article-service/src/main/java/com/articleservice/service/ArticleService.java`（`getUserName` 方法，约第 669 行）
- **描述**: `catch (Exception ignored) {}` 吞掉所有异常无日志，Feign 超时、服务不可用、序列化错误等完全不可观测，排查问题时无从入手。
- **建议**: 至少加一行 `log.warn("获取用户名失败, userId={}", userId, e)`。

### 13. [架构] ArticleService 混合 Service 和基础设施关注点

- **文件**: `article-service/src/main/java/com/articleservice/service/ArticleService.java`
- **描述**: 直接 `@Autowired RabbitTemplate` 和 `UserClient`，通知发送和 Feign 调用逻辑混在 Service 层，业务编排与基础设施耦合。
- **建议**: 将通知逻辑移至已有的 `ArticleAsyncService` 或新建 `ArticleNotifyService`。

### 14. [CLAUDE.md] DbWriteAuditLogger 单行超过 120 字符

- **文件**: `blog-common/src/main/java/com/blogcommon/logging/DbWriteAuditLogger.java`
- **描述**: 第 29 行（161 字符）、第 35 行（122 字符）、第 88 行（156 字符）超过 CLAUDE.md 规定的 120 字符限制。
- **建议**: 换行处理以符合格式规范。

### 15. [OOP] NotifyService 包装类型使用 == 比较

- **文件**: `notify-service/src/main/java/com/notifyservice/service/NotifyService.java`（约第 896、926 行）
- **描述**: `notify.getIsRead() == 1` 和 `notify.getIsRead() == 0` 使用 `==` 比较 Integer 包装类型，虽然有 null 前置检查避免了 NPE，但不符合 CLAUDE.md "包装类型比较必须用 equals()" 规范。
- **建议**: 改为 `Integer.valueOf(1).equals(notify.getIsRead())`。

---

## 统计

| 风险等级 | 数量 |
|---------|------|
| 高 | 3 |
| 中 | 8 |
| 低 | 4 |
| **合计** | **15** |

| 审查维度 | 发现数量 |
|---------|---------|
| 安全 | 4 |
| CLAUDE.md 合规 | 4 |
| 架构设计 | 3 |
| Bug | 3 |
| 配置 | 3 |
| 测试 | 1 |

---

## 优先修复建议

1. **立即处理**：CORS 配置、.run/ 密码泄露（安全风险最高）
2. **尽快处理**：SELECT * 违规、Nacos 端口不一致、前端错误处理缺失（影响代码质量和可维护性）
3. **计划处理**：UserClient fallback、热路径 Feign 解耦、密码重置二次验证（架构和安全改进）
4. **持续改进**：补充测试覆盖、异常日志、编码规范细节
