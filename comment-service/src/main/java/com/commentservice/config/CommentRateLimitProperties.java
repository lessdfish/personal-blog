package com.commentservice.config;

import com.blogcommon.config.RefreshablePropertiesMarker;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * 评论限流配置：集中管理评论频率窗口、阈值和开关，支持 Nacos 动态刷新。
 */
@Component
@RefreshScope
@ConfigurationProperties(prefix = "comment.rate-limit")
public class CommentRateLimitProperties implements RefreshablePropertiesMarker {
    private boolean enabled = true;
    private long windowSeconds = 60;
    private int threshold = 10;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getWindowSeconds() {
        return windowSeconds;
    }

    public void setWindowSeconds(long windowSeconds) {
        this.windowSeconds = windowSeconds;
    }

    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }
}
