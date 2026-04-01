package com.blogcommon.util;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * ClassName:RedisLockUtil
 * Package:com.blogcommon.util
 * Description:Redis分布式锁工具类
 *
 * @Author:lyp
 * @Create:2026/4/1
 * @Version: v1.0
 */
public class RedisLockUtil {

    private static final String LOCK_SCRIPT = 
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "    return redis.call('del', KEYS[1]) " +
            "else " +
            "    return 0 " +
            "end";

    /**
     * 尝试获取分布式锁
     * @param redisTemplate Redis模板
     * @param lockKey 锁的key
     * @param expireSeconds 过期时间（秒）
     * @return 锁的value（用于释放锁），null表示获取失败
     */
    public static String tryLock(StringRedisTemplate redisTemplate, String lockKey, long expireSeconds) {
        String lockValue = UUID.randomUUID().toString();
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, expireSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success) ? lockValue : null;
    }

    /**
     * 释放分布式锁（Lua脚本保证原子性）
     * @param redisTemplate Redis模板
     * @param lockKey 锁的key
     * @param lockValue 锁的value
     * @return 是否释放成功
     */
    public static boolean unlock(StringRedisTemplate redisTemplate, String lockKey, String lockValue) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(LOCK_SCRIPT, Long.class);
        Long result = redisTemplate.execute(script, 
                Collections.singletonList(lockKey), 
                lockValue);
        return Long.valueOf(1L).equals(result);
    }

    /**
     * 尝试获取锁（带重试）
     * @param redisTemplate Redis模板
     * @param lockKey 锁的key
     * @param expireSeconds 过期时间（秒）
     * @param retryTimes 重试次数
     * @param retryInterval 重试间隔（毫秒）
     * @return 锁的value，null表示获取失败
     */
    public static String tryLockWithRetry(StringRedisTemplate redisTemplate, 
                                          String lockKey, 
                                          long expireSeconds,
                                          int retryTimes,
                                          long retryInterval) {
        for (int i = 0; i < retryTimes; i++) {
            String lockValue = tryLock(redisTemplate, lockKey, expireSeconds);
            if (lockValue != null) {
                return lockValue;
            }
            if (i < retryTimes - 1) {
                try {
                    Thread.sleep(retryInterval);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }
        return null;
    }
}
