package com.bloggateway.service;

import com.blogcommon.constant.RedisKeyConstants;
import com.bloggateway.config.IpSecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IpBlocklistServiceTest {
    private final ReactiveStringRedisTemplate redisTemplate = mock(ReactiveStringRedisTemplate.class);
    private final IpSecurityProperties properties = new IpSecurityProperties();
    private final IpBlocklistService service = new IpBlocklistService(redisTemplate, properties);

    @Test
    void isBlockedShouldReadExpectedRedisKey() {
        when(redisTemplate.hasKey(RedisKeyConstants.SECURITY_BLOCK_IP_KEY + "203.0.113.7")).thenReturn(Mono.just(true));

        assertTrue(service.isBlocked("203.0.113.7").block());
    }

    @Test
    void isBlockedShouldAllowBlankIp() {
        assertFalse(service.isBlocked("").block());
    }

    @Test
    void blockShouldSetTtlKey() {
        @SuppressWarnings("unchecked")
        ReactiveValueOperations<String, String> valueOperations = mock(ReactiveValueOperations.class);
        properties.getBlocklist().setTtlSeconds(30);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.set(RedisKeyConstants.SECURITY_BLOCK_IP_KEY + "203.0.113.8", "1", Duration.ofSeconds(30)))
                .thenReturn(Mono.just(true));

        assertTrue(service.block("203.0.113.8").block());
        verify(valueOperations).set(RedisKeyConstants.SECURITY_BLOCK_IP_KEY + "203.0.113.8", "1", Duration.ofSeconds(30));
    }
}
