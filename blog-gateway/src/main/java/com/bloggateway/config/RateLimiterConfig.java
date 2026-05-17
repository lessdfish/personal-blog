package com.bloggateway.config;

import com.bloggateway.service.ClientIpResolver;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {
    private final ClientIpResolver clientIpResolver;

    /**
     * 构造 RateLimiterConfig：注入这个类运行时需要的依赖。
     */
    public RateLimiterConfig(ClientIpResolver clientIpResolver) {
        this.clientIpResolver = clientIpResolver;
    }

    /**
     * 创建限流 key 解析器：按客户端 IP 区分不同用户的访问频率。
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(clientIpResolver.resolve(exchange));
    }
}
