package com.notifyservice.cache;

import com.blogcommon.cache.CacheInvalidationMessage;
import com.blogcommon.cache.CacheKeyResolver;
import com.blogcommon.constant.RedisKeyConstants;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * 通知缓存键解析器：把通知表变更转换为用户未读数缓存失效键。
 */
@Component
public class NotifyCacheKeyResolver implements CacheKeyResolver {
    @Override
    public boolean supports(String tableName) {
        return "tb_notify".equals(tableName);
    }

    @Override
    public Collection<String> resolveKeys(CacheInvalidationMessage message) {
        String userId = message.getColumns() == null ? null : message.getColumns().get("user_id");
        if (userId == null || userId.isBlank()) {
            return List.of(RedisKeyConstants.NOTIFY_UNREAD_KEY);
        }
        return List.of(RedisKeyConstants.NOTIFY_UNREAD_KEY + userId);
    }
}
