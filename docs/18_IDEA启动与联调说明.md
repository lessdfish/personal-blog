# IDEA 启动与联调说明

## 1. 已生成的 IDEA 运行配置

我已经在项目根目录下生成了 `.run` 配置，你在 IDEA 里刷新项目后可以直接看到：

- `01 UserService`
- `02 ArticleService`
- `03 CommentService`
- `04 NotifyService`
- `05 BlogGateway`
- `06 AllServices`

对应文件位置：

- [01_UserService.run.xml](/F:/test_file/blog-cloud/.run/01_UserService.run.xml)
- [02_ArticleService.run.xml](/F:/test_file/blog-cloud/.run/02_ArticleService.run.xml)
- [03_CommentService.run.xml](/F:/test_file/blog-cloud/.run/03_CommentService.run.xml)
- [04_NotifyService.run.xml](/F:/test_file/blog-cloud/.run/04_NotifyService.run.xml)
- [05_BlogGateway.run.xml](/F:/test_file/blog-cloud/.run/05_BlogGateway.run.xml)
- [06_AllServices.run.xml](/F:/test_file/blog-cloud/.run/06_AllServices.run.xml)

## 2. 启动前准备

先确认 Docker 里的基础中间件都已经启动：

- MySQL
- Redis
- RabbitMQ
- Nacos

你当前本机配置已经按下面这组参数写进 IDEA 运行配置：

- MySQL：`blog_cloud_test`
- MySQL 用户：`root`
- MySQL 密码：`032581`
- Redis：`127.0.0.1:6379`
- RabbitMQ：`127.0.0.1:35672`
- Nacos：`127.0.0.1:8848`

## 3. 推荐启动顺序

如果你不用 `06 AllServices` 一键启动，建议按顺序点：

1. `01 UserService`
2. `02 ArticleService`
3. `03 CommentService`
4. `04 NotifyService`
5. `05 BlogGateway`

## 4. 启动成功后检查

### 端口

- `9011` user-service
- `9020` article-service
- `9003` comment-service
- `9004` notify-service
- `8080` blog-gateway

### Swagger

- `http://localhost:9011/swagger-ui.html`
- `http://localhost:9020/swagger-ui.html`
- `http://localhost:9003/swagger-ui.html`
- `http://localhost:9004/swagger-ui.html`

### Nacos

- `http://localhost:8848/nacos`

检查注册列表里是否有：

- `user-service`
- `article-service`
- `comment-service`
- `notify-service`
- `blog-gateway`

## 5. 联调建议

启动成功后先测试这条最小闭环：

1. 登录
2. 查询文章列表
3. 查询文章详情
4. 发表评论
5. 查询通知
6. 退出登录
7. 再带旧 token 请求一次受保护接口，确认被拒绝

## 6. 这次认证链路改动后的效果

当前登录和退出的效果是：

- 登录成功后，token 会缓存到 Redis
- 网关转发时会携带 `Authorization`
- `comment-service` 通过 Feign 远程调用时会自动透传 `Authorization`
- 各微服务会在解析 JWT 后，再去 Redis 校验 token 是否仍然有效
- 退出登录后，`user-service` 删除 Redis 中的当前 token
- 之后同一个旧 token 在文章、评论、通知、用户服务里都会失效

这意味着现在的效果不是“每个微服务自己存一份 token 再逐个删除”，而是：

`通过共享 Redis 会话态实现统一失效`

这是更适合微服务的做法。

