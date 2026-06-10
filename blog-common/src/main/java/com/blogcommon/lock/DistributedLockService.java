package com.blogcommon.lock;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * 分布式锁服务：提供带超时、重试和安全释放的业务锁接口，隐藏 Redis 实现细节。
 */
public interface DistributedLockService {
    String tryLock(String lockKey, Duration expire);

    boolean unlock(String lockKey, String lockValue);

    <T> T executeWithLock(String lockKey, Duration expire, Supplier<T> action, Supplier<T> lockFailed);
}
