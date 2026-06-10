package com.blogcommon.lock;

import com.blogcommon.util.RedisLockUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Redis 分布式锁实现：基于 set-if-absent 和 Lua 释放锁，适配 Sentinel 模式下的 StringRedisTemplate。
 */
@Component
public class RedisDistributedLockService implements DistributedLockService {
    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public String tryLock(String lockKey, Duration expire) {
        if (stringRedisTemplate == null) {
            return "local";
        }
        long seconds = Math.max(1, expire.toSeconds());
        return RedisLockUtil.tryLock(stringRedisTemplate, lockKey, seconds);
    }

    @Override
    public boolean unlock(String lockKey, String lockValue) {
        if (stringRedisTemplate == null) {
            return true;
        }
        if (!StringUtils.hasText(lockValue)) {
            return false;
        }
        return RedisLockUtil.unlock(stringRedisTemplate, lockKey, lockValue);
    }

    @Override
    public <T> T executeWithLock(String lockKey, Duration expire, Supplier<T> action, Supplier<T> lockFailed) {
        String lockValue = tryLock(lockKey, expire);
        if (!StringUtils.hasText(lockValue)) {
            return lockFailed.get();
        }
        try {
            return action.get();
        } finally {
            unlock(lockKey, lockValue);
        }
    }
}
