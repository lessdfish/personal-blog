package com.blogcommon.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MultiLevelCacheServiceTest {
    @Test
    void getShouldUseLocalCacheBeforeLoader() {
        CacheProperties properties = new CacheProperties();
        LocalCacheService localCacheService = new LocalCacheService(properties);
        MultiLevelCacheService service = new MultiLevelCacheService(properties, localCacheService, new ObjectMapper());
        AtomicInteger loads = new AtomicInteger();

        assertEquals("value", service.get("k1", String.class, () -> {
            loads.incrementAndGet();
            return "value";
        }));
        assertEquals("value", service.get("k1", String.class, () -> "other"));

        assertEquals(1, loads.get());
    }

    @Test
    void getShouldUseRedisBeforeLoader() {
        CacheProperties properties = new CacheProperties();
        LocalCacheService localCacheService = new LocalCacheService(properties);
        MultiLevelCacheService service = new MultiLevelCacheService(properties, localCacheService, new ObjectMapper());
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("k2")).thenReturn("\"redis-value\"");
        ReflectionTestUtils.setField(service, "stringRedisTemplate", redisTemplate);

        assertEquals("redis-value", service.get("k2", String.class, () -> "loader-value"));
        verify(valueOperations, never()).set(eq("k2"), eq("\"loader-value\""), org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void evictShouldRemoveLocalAndRedisCache() {
        CacheProperties properties = new CacheProperties();
        LocalCacheService localCacheService = new LocalCacheService(properties);
        MultiLevelCacheService service = new MultiLevelCacheService(properties, localCacheService, new ObjectMapper());
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        ReflectionTestUtils.setField(service, "stringRedisTemplate", redisTemplate);
        service.put("k3", "value");

        service.evict("k3");

        assertEquals("new", service.get("k3", String.class, () -> "new"));
        verify(redisTemplate).delete("k3");
    }
}
