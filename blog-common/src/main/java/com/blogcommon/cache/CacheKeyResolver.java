package com.blogcommon.cache;

import java.util.Collection;

/**
 * 缓存键解析器：把数据库表变更转换成需要清理的 Redis key 和本地缓存 key。
 */
public interface CacheKeyResolver {
    boolean supports(String tableName);

    Collection<String> resolveKeys(CacheInvalidationMessage message);
}
