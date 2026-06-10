package com.blogcommon.cache;

import com.blogcommon.constant.RedisKeyConstants;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.io.IOException;
import java.util.List;

/**
 * 缓存失效订阅配置：监听 Redis Pub/Sub 消息并清理本 JVM 的本地缓存。
 */
@Configuration
@ConditionalOnBean(RedisConnectionFactory.class)
public class CacheInvalidationSubscriberConfig {
    @Bean
    public RedisMessageListenerContainer cacheInvalidationMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            LocalCacheService localCacheService,
            ObjectMapper objectMapper) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(
                cacheInvalidationMessageListener(localCacheService, objectMapper),
                new ChannelTopic(RedisKeyConstants.CACHE_INVALIDATION_CHANNEL)
        );
        return container;
    }

    private MessageListener cacheInvalidationMessageListener(LocalCacheService localCacheService,
                                                            ObjectMapper objectMapper) {
        return (message, pattern) -> {
            try {
                List<String> keys = objectMapper.readValue(message.getBody(), new TypeReference<>() {
                });
                for (String key : keys) {
                    localCacheService.evict(key);
                    localCacheService.evictByPrefix(key);
                }
            } catch (IOException ignored) {
            }
        };
    }
}
