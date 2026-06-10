package com.blogcommon.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 多级缓存服务：按 L1 本地缓存 -> L2 Redis -> 数据库加载器 的顺序读数据，并统一写回与失效。
 */
@Component
public class MultiLevelCacheService {
    private final CacheProperties properties;
    private final LocalCacheService localCacheService;
    private final ObjectMapper objectMapper;
    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    public MultiLevelCacheService(CacheProperties properties,
                                  LocalCacheService localCacheService,
                                  ObjectMapper objectMapper) {
        this.properties = properties;
        this.localCacheService = localCacheService;
        this.objectMapper = objectMapper;
    }

    public <T> T get(String key, Class<T> targetType, Supplier<T> loader) {
        if (!properties.isEnabled()) {
            return loader.get();
        }
        T localValue = localCacheService.get(key, targetType);
        if (localValue != null) {
            return localValue;
        }
        T redisValue = readRedis(key, targetType);
        if (redisValue != null) {
            localCacheService.put(key, redisValue);
            return redisValue;
        }
        T loaded = loader.get();
        put(key, loaded);
        return loaded;
    }

    public void put(String key, Object value) {
        if (!properties.isEnabled() || value == null) {
            return;
        }
        localCacheService.put(key, value);
        writeRedis(key, value);
    }

    public void evict(String key) {
        localCacheService.evict(key);
        if (stringRedisTemplate != null) {
            stringRedisTemplate.delete(key);
        }
    }

    public void evictByPrefix(String prefix) {
        localCacheService.evictByPrefix(prefix);
    }

    private <T> T readRedis(String key, Class<T> targetType) {
        if (!properties.isRedisEnabled() || stringRedisTemplate == null) {
            return null;
        }
        String json = stringRedisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, targetType);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private void writeRedis(String key, Object value) {
        if (!properties.isRedisEnabled() || stringRedisTemplate == null) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue().set(
                    key,
                    objectMapper.writeValueAsString(value),
                    Math.max(1, properties.getRedisTtlSeconds()),
                    TimeUnit.SECONDS
            );
        } catch (JsonProcessingException ignored) {
        }
    }
}
