package com.bloggateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "security.guard")
public class SecurityGuardProperties {
    private boolean enabled = true;
    private long maxRequestBodyBytes = 10 * 1024 * 1024;
    private int maxPathLength = 2048;
    private int maxQueryLength = 4096;
    private int maxHeaderLength = 4096;

    /**
     * 判断 enabled：返回当前开关或状态是否成立。
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置 enabled：把外部传入的值保存到当前对象。
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 获取 maxRequestBodyBytes：返回当前对象里保存的这个值。
     */
    public long getMaxRequestBodyBytes() {
        return maxRequestBodyBytes;
    }

    /**
     * 设置 maxRequestBodyBytes：把外部传入的值保存到当前对象。
     */
    public void setMaxRequestBodyBytes(long maxRequestBodyBytes) {
        this.maxRequestBodyBytes = maxRequestBodyBytes;
    }

    /**
     * 获取 maxPathLength：返回当前对象里保存的这个值。
     */
    public int getMaxPathLength() {
        return maxPathLength;
    }

    /**
     * 设置 maxPathLength：把外部传入的值保存到当前对象。
     */
    public void setMaxPathLength(int maxPathLength) {
        this.maxPathLength = maxPathLength;
    }

    /**
     * 获取 maxQueryLength：返回当前对象里保存的这个值。
     */
    public int getMaxQueryLength() {
        return maxQueryLength;
    }

    /**
     * 设置 maxQueryLength：把外部传入的值保存到当前对象。
     */
    public void setMaxQueryLength(int maxQueryLength) {
        this.maxQueryLength = maxQueryLength;
    }

    /**
     * 获取 maxHeaderLength：返回当前对象里保存的这个值。
     */
    public int getMaxHeaderLength() {
        return maxHeaderLength;
    }

    /**
     * 设置 maxHeaderLength：把外部传入的值保存到当前对象。
     */
    public void setMaxHeaderLength(int maxHeaderLength) {
        this.maxHeaderLength = maxHeaderLength;
    }
}
