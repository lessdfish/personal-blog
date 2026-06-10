package com.blogcommon.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * 多级缓存配置属性：统一管理本地缓存容量、TTL、Redis TTL 和缓存开关，支持 Nacos 热更新。
 */
@Component
@RefreshScope
@ConfigurationProperties(prefix = "blog.cache")
public class CacheProperties {
    private boolean enabled = true;
    private boolean localEnabled = true;
    private boolean redisEnabled = true;
    private long localMaximumSize = 1000;
    private long localTtlSeconds = 300;
    private long redisTtlSeconds = 600;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isLocalEnabled() {
        return localEnabled;
    }

    public void setLocalEnabled(boolean localEnabled) {
        this.localEnabled = localEnabled;
    }

    public boolean isRedisEnabled() {
        return redisEnabled;
    }

    public void setRedisEnabled(boolean redisEnabled) {
        this.redisEnabled = redisEnabled;
    }

    public long getLocalMaximumSize() {
        return localMaximumSize;
    }

    public void setLocalMaximumSize(long localMaximumSize) {
        this.localMaximumSize = localMaximumSize;
    }

    public long getLocalTtlSeconds() {
        return localTtlSeconds;
    }

    public void setLocalTtlSeconds(long localTtlSeconds) {
        this.localTtlSeconds = localTtlSeconds;
    }

    public long getRedisTtlSeconds() {
        return redisTtlSeconds;
    }

    public void setRedisTtlSeconds(long redisTtlSeconds) {
        this.redisTtlSeconds = redisTtlSeconds;
    }
}
