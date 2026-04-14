package com.blogcommon.auth;

import com.blogcommon.constant.RedisKeyConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenSessionValidator {
    private static final Logger log = LoggerFactory.getLogger(TokenSessionValidator.class);
    private final Map<String, Long> localTokenCache = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;
    @Value("${auth.token-session.fail-open-on-redis-error:true}")
    private boolean failOpenOnRedisError;
    @Value("${auth.token-session.local-cache-seconds:300}")
    private long localCacheSeconds;

    public boolean isTokenActive(Long userId, String token) {
        if (userId == null || token == null || token.isBlank()) {
            return false;
        }
        if (stringRedisTemplate == null) {
            return true;
        }
        try {
            String cachedToken = stringRedisTemplate.opsForValue().get(RedisKeyConstants.USER_TOKEN_KEY + userId);
            boolean active = token.equals(cachedToken);
            if (active) {
                rememberLocalToken(userId, token);
            }
            return active;
        } catch (Exception e) {
            log.warn("Redis token validation failed, userId={}, fallback={}", userId, failOpenOnRedisError, e);
            if (isRememberedLocally(userId, token)) {
                return true;
            }
            return failOpenOnRedisError;
        }
    }

    private void rememberLocalToken(Long userId, String token) {
        localTokenCache.put(buildLocalKey(userId, token), System.currentTimeMillis() + localCacheSeconds * 1000);
    }

    private boolean isRememberedLocally(Long userId, String token) {
        String key = buildLocalKey(userId, token);
        Long expireAt = localTokenCache.get(key);
        if (expireAt == null) {
            return false;
        }
        if (expireAt < System.currentTimeMillis()) {
            localTokenCache.remove(key);
            return false;
        }
        return true;
    }

    private String buildLocalKey(Long userId, String token) {
        return RedisKeyConstants.USER_TOKEN_KEY + userId + ":" + token;
    }
}
