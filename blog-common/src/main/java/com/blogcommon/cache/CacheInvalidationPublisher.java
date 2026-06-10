package com.blogcommon.cache;

import com.blogcommon.constant.RedisKeyConstants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * 缓存失效发布器：通过 Redis Pub/Sub 通知各服务清理本地一级缓存。
 */
@Component
public class CacheInvalidationPublisher {
    private final ObjectMapper objectMapper;
    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    public CacheInvalidationPublisher(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void publish(Collection<String> keys) {
        if (stringRedisTemplate == null || keys == null || keys.isEmpty()) {
            return;
        }
        try {
            stringRedisTemplate.convertAndSend(
                    RedisKeyConstants.CACHE_INVALIDATION_CHANNEL,
                    objectMapper.writeValueAsString(keys)
            );
        } catch (JsonProcessingException ignored) {
        }
    }
}
