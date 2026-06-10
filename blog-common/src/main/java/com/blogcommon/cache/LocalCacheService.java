package com.blogcommon.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 本地缓存服务：封装 Caffeine 读写和失效逻辑，作为 Redis 之前的一级缓存。
 */
@Component
public class LocalCacheService {
    private final Cache<String, Object> cache;
    private final CacheProperties properties;

    public LocalCacheService(CacheProperties properties) {
        this.properties = properties;
        this.cache = Caffeine.newBuilder()
                .maximumSize(Math.max(1, properties.getLocalMaximumSize()))
                .expireAfterWrite(Math.max(1, properties.getLocalTtlSeconds()), TimeUnit.SECONDS)
                .build();
    }

    public <T> T get(String key, Class<T> targetType) {
        if (!properties.isEnabled() || !properties.isLocalEnabled()) {
            return null;
        }
        Object value = cache.getIfPresent(key);
        if (value == null || !targetType.isInstance(value)) {
            return null;
        }
        return targetType.cast(value);
    }

    public void put(String key, Object value) {
        if (!properties.isEnabled() || !properties.isLocalEnabled() || value == null) {
            return;
        }
        cache.put(key, value);
    }

    public void evict(String key) {
        cache.invalidate(key);
    }

    public void evictByPrefix(String prefix) {
        cache.asMap().keySet().removeIf(key -> key.startsWith(prefix));
    }
}
