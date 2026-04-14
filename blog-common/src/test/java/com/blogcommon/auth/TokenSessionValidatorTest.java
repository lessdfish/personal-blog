package com.blogcommon.auth;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

class TokenSessionValidatorTest {

    @Test
    void shouldReturnFalseWhenUserIdOrTokenInvalid() {
        TokenSessionValidator validator = new TokenSessionValidator();

        assertFalse(validator.isTokenActive(null, "token"));
        assertFalse(validator.isTokenActive(1L, null));
        assertFalse(validator.isTokenActive(1L, " "));
    }

    @Test
    void shouldReturnTrueWhenRedisUnavailable() {
        TokenSessionValidator validator = new TokenSessionValidator();
        assertTrue(validator.isTokenActive(1L, "token"));
    }

    @Test
    void shouldValidateCachedToken() {
        TokenSessionValidator validator = new TokenSessionValidator();
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("blog:user:token:1")).thenReturn("token-1");
        ReflectionTestUtils.setField(validator, "stringRedisTemplate", template);

        assertTrue(validator.isTokenActive(1L, "token-1"));
        assertFalse(validator.isTokenActive(1L, "token-2"));
    }

    @Test
    void shouldFallbackToLocalCacheWhenRedisFails() {
        TokenSessionValidator validator = new TokenSessionValidator();
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("blog:user:token:2")).thenReturn("token-2");
        ReflectionTestUtils.setField(validator, "stringRedisTemplate", template);
        ReflectionTestUtils.setField(validator, "failOpenOnRedisError", false);
        ReflectionTestUtils.setField(validator, "localCacheSeconds", 300L);

        assertTrue(validator.isTokenActive(2L, "token-2"));

        doThrow(new RuntimeException("redis down")).when(valueOperations).get("blog:user:token:2");
        assertTrue(validator.isTokenActive(2L, "token-2"));
    }

    @Test
    void shouldHonorFailOpenWhenRedisFailsWithoutLocalCache() {
        TokenSessionValidator validator = new TokenSessionValidator();
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(valueOperations);
        doThrow(new RuntimeException("redis down")).when(valueOperations).get("blog:user:token:3");
        ReflectionTestUtils.setField(validator, "stringRedisTemplate", template);
        ReflectionTestUtils.setField(validator, "failOpenOnRedisError", true);

        assertTrue(validator.isTokenActive(3L, "token-3"));
    }

    @Test
    void shouldReturnFalseWhenRedisFailsAndNoCacheAndFailOpenDisabled() {
        TokenSessionValidator validator = new TokenSessionValidator();
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(valueOperations);
        doThrow(new RuntimeException("redis down")).when(valueOperations).get("blog:user:token:4");
        ReflectionTestUtils.setField(validator, "stringRedisTemplate", template);
        ReflectionTestUtils.setField(validator, "failOpenOnRedisError", false);

        assertFalse(validator.isTokenActive(4L, "token-4"));
    }

    @Test
    void shouldReturnFalseWhenLocalCacheExpired() {
        TokenSessionValidator validator = new TokenSessionValidator();
        ReflectionTestUtils.setField(validator, "stringRedisTemplate", mock(StringRedisTemplate.class));
        ReflectionTestUtils.setField(validator, "failOpenOnRedisError", false);
        ReflectionTestUtils.setField(validator, "localCacheSeconds", 300L);
        @SuppressWarnings("unchecked")
        java.util.Map<String, Long> localTokenCache =
                (java.util.Map<String, Long>) ReflectionTestUtils.getField(validator, "localTokenCache");
        localTokenCache.put("blog:user:token:5:token-5", System.currentTimeMillis() - 1000);

        assertFalse(validator.isTokenActive(5L, "token-5"));
    }
}
