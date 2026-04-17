项目路径：`F:\test_file\blog-cloud`

这是一个基于 `Spring Boot + Spring Cloud + MySQL + Redis + RabbitMQ + Nacos + Gateway + MyBatis + PageHelper + Docker` 的博客论坛后端项目，已经拆成多模块微服务：
- `blog-gateway`
- `user-service`
- `article-service`
- `comment-service`
- `notify-service`
- `blog-common`

当前状态：
- Docker 中间件已可用：`Redis`、`RabbitMQ`、`Nacos`
- 微服务已支持通过 `Nacos` 注册发现
- 网关按服务名 `lb://service-name` 路由，不再依赖固定端口
- 业务服务端口已改为自动选择空闲端口：`server.port=${SERVER_PORT:0}`
- 网关固定 `8080`

重要约束：
- 不要修改 `ResultCode.java`
- 不要因为中文显示问题去改我的中文内容
- Maven 版本和配置不要乱动
- 命令尽量控制在 20 秒以内，超时就换下一个

已经完成的核心功能：
- 用户注册、登录、退出、当前用户信息
- JWT + Redis 会话校验
- 退出登录后 token 在各微服务统一失效
- 评论、回复、同级回复
- 评论后通过 RabbitMQ 发送通知，通知服务落库
- 通知分页、未读数、已读、删除
- 文章发布、详情、普通分页、热榜分页
- 点赞、收藏改成显式 `PUT/DELETE` 幂等接口
- Redis 做文章热度、活跃用户、未读数、点赞/收藏状态缓存
- 通知、分页等索引和部分性能优化已完成
- 简单的前端调试页已放在网关静态资源下：`/api-debug/*`

重要实现细节：
1. 登录态
- 原本是前端拿 JWT 放 `Authorization`
- 现在已改成 `HttpOnly Cookie`
- 登录成功后，`user-service` 把 JWT 写入 Cookie
- 返回给前端的 JSON 不再暴露 token，只返回：
  - `username`
  - `nickname`
  - `avatar`
- 网关从 Cookie 中取出 JWT，再补成 `Authorization` 头转发给下游
- 各微服务本质上仍然校验 JWT + Redis 会话，不是直接信任 Cookie

相关文件：
- `blog-common/src/main/java/com/blogcommon/auth/AuthConstants.java`
- `blog-common/src/main/java/com/blogcommon/auth/JwtAuthSupport.java`
- `blog-gateway/src/main/java/com/bloggateway/filter/GatewayAuthFilter.java`
- `user-service/src/main/java/com/userservice/controller/UserTestController.java`
- `user-service/src/main/java/com/userservice/vo/LoginVO.java`

2. 同级回复
- 回复子评论时，后端会把 `parentId` 归并到根评论，实现“同级回复”
- 但通知仍然发给实际被回复的人

相关文件：
- `comment-service/src/main/java/com/commentservice/service/CommentService.java`
- `comment-service/src/test/java/com/commentservice/service/CommentServiceTest.java`

3. 前端调试页
- 位置：`blog-gateway/src/main/resources/static/api-debug/`
- 当前只做按钮调用接口并看 JSON 返回值，不是正式前端
- 登录后不再展示 token，只显示登录成功、username、avatar；点击 username 展开 nickname

4. 连接池和线程池
- 网关已配置 Reactor Netty HTTP 连接池
- `article-service` 已加入线程池，用于热榜缓存异步预热和热榜缓存异步清理

相关文件：
- `blog-gateway/src/main/resources/application.yml`
- `article-service/src/main/java/com/articleservice/config/AsyncConfig.java`
- `article-service/src/main/java/com/articleservice/service/ArticleAsyncService.java`
- `article-service/src/main/java/com/articleservice/service/ArticleService.java`

5. Feign 透传
- `comment-service` 调其他服务时，优先透传 `Authorization`
- 如果请求头没有，则从 Cookie 中取 JWT 再转成 `Authorization`

相关文件：
- `comment-service/src/main/java/com/commentservice/config/FeignConfig.java`

已验证情况：
- `user-service -am compile` 通过
- `blog-gateway -am compile` 通过
- `article-service -am compile` 通过
- `comment-service -am compile` 有一次因 20 秒限制超时，不代表必然失败
- 多份测试、压测、部署、CI/CD、性能优化、需求分析文档已经生成在 `docs/` 目录

下一步工作建议：
- 继续做真实联调验证：Cookie 登录 -> 文章/评论/通知访问 -> 退出后失效
- 再逐步进入真正前端开发或上线部署收口