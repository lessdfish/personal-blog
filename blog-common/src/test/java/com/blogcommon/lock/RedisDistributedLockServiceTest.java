package com.blogcommon.lock;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedisDistributedLockServiceTest {
    @Test
    void tryLockShouldReturnValueWhenRedisAcceptsLock() {
        RedisDistributedLockService service = new RedisDistributedLockService();
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), org.mockito.ArgumentMatchers.eq(3L), org.mockito.ArgumentMatchers.eq(TimeUnit.SECONDS)))
                .thenReturn(true);
        ReflectionTestUtils.setField(service, "stringRedisTemplate", redisTemplate);

        assertNotNull(service.tryLock("lock:test", Duration.ofSeconds(3)));
    }

    @Test
    void tryLockShouldReturnNullWhenRedisRejectsLock() {
        RedisDistributedLockService service = new RedisDistributedLockService();
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), org.mockito.ArgumentMatchers.eq(3L), org.mockito.ArgumentMatchers.eq(TimeUnit.SECONDS)))
                .thenReturn(false);
        ReflectionTestUtils.setField(service, "stringRedisTemplate", redisTemplate);

        assertNull(service.tryLock("lock:test", Duration.ofSeconds(3)));
    }

    @Test
    void executeWithLockShouldUseFallbackWhenLockFails() {
        DistributedLockService service = new RedisDistributedLockService();

        assertEquals("action", service.executeWithLock("k", Duration.ofSeconds(1), () -> "action", () -> "fallback"));
    }
}
