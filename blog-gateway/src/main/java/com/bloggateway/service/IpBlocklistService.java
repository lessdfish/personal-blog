package com.bloggateway.service;

import com.blogcommon.constant.RedisKeyConstants;
import com.bloggateway.config.IpSecurityProperties;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class IpBlocklistService {
    private final ReactiveStringRedisTemplate redisTemplate;
    private final IpSecurityProperties properties;

    /**
     * 构造 IpBlocklistService：注入这个类运行时需要的依赖。
     */
    public IpBlocklistService(ReactiveStringRedisTemplate redisTemplate, IpSecurityProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    /**
     * 判断 IP 是否在封禁列表中。
     */
    public Mono<Boolean> isBlocked(String ip) {
        if (!StringUtils.hasText(ip)) {
            return Mono.just(false);
        }
        return redisTemplate.hasKey(blockKey(ip)).onErrorReturn(false);
    }

    /**
     * 封禁 IP：把 IP 写入 Redis 并设置过期时间。
     */
    public Mono<Boolean> block(String ip) {
        if (!StringUtils.hasText(ip)) {
            return Mono.just(false);
        }
        long ttlSeconds = Math.max(properties.getBlocklist().getTtlSeconds(), 1);
        return redisTemplate.opsForValue()
                .set(blockKey(ip), "1", Duration.ofSeconds(ttlSeconds))
                .onErrorReturn(false);
    }

    /**
     * 生成 IP 封禁缓存 key。
     */
    private String blockKey(String ip) {
        return RedisKeyConstants.SECURITY_BLOCK_IP_KEY + ip;
    }
}
