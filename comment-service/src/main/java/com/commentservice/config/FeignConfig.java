package com.commentservice.config;

import com.blogcommon.feign.CommonFeignConfig;
import org.springframework.context.annotation.Configuration;

/**
 * 评论服务 Feign 配置：复用公共 token 透传、错误解析和日志级别。
 */
@Configuration
public class FeignConfig extends CommonFeignConfig {
}
