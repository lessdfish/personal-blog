package com.canalservice.handler;

import com.blogcommon.cache.CacheInvalidationMessage;
import com.blogcommon.cache.CacheInvalidationPublisher;
import com.blogcommon.constant.RedisKeyConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 缓存失效处理器：根据表名和主键清理 Redis 缓存，并发布本地缓存失效事件。
 */
@Slf4j
@Component
public class CacheInvalidationHandler {
    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;
    private final CacheInvalidationPublisher cacheInvalidationPublisher;

    public CacheInvalidationHandler(CacheInvalidationPublisher cacheInvalidationPublisher) {
        this.cacheInvalidationPublisher = cacheInvalidationPublisher;
    }

    public void handle(CacheInvalidationMessage message) {
        Collection<String> keys = resolveKeys(message);
        if (keys.isEmpty()) {
            return;
        }
        if (stringRedisTemplate != null) {
            List<String> exactKeys = keys.stream()
                    .filter(key -> !key.endsWith(":"))
                    .toList();
            if (!exactKeys.isEmpty()) {
                stringRedisTemplate.delete(exactKeys);
            }
        }
        cacheInvalidationPublisher.publish(keys);
        log.info("Canal cache invalidated, table={}, keys={}", message.getTableName(), keys);
    }

    private Collection<String> resolveKeys(CacheInvalidationMessage message) {
        List<String> keys = new ArrayList<>();
        String table = message.getTableName();
        String primaryKey = message.getPrimaryKey();
        String articleId = message.getColumns() == null ? null : message.getColumns().get("article_id");
        String userId = message.getColumns() == null ? null : message.getColumns().get("user_id");
        String roleId = message.getColumns() == null ? null : message.getColumns().get("role_id");

        if ("tb_article".equals(table) && hasText(primaryKey)) {
            keys.add(RedisKeyConstants.ARTICLE_DETAIL_CACHE_KEY + primaryKey);
            keys.add(RedisKeyConstants.ARTICLE_HEAT_KEY + primaryKey);
        }
        if (List.of("tb_article_like", "tb_article_favorite", "tb_comment").contains(table) && hasText(articleId)) {
            keys.add(RedisKeyConstants.ARTICLE_DETAIL_CACHE_KEY + articleId);
            keys.add(RedisKeyConstants.ARTICLE_HEAT_KEY + articleId);
        }
        if (List.of("tb_article", "tb_board", "tb_article_like", "tb_article_favorite", "tb_comment").contains(table)) {
            keys.add(RedisKeyConstants.ARTICLE_HOT_CACHE_KEY);
        }
        if (List.of("tb_role_permission", "tb_role", "tb_permission").contains(table)) {
            keys.add(RedisKeyConstants.ROLE_PERMISSION_BY_ID_KEY);
            keys.add(RedisKeyConstants.ROLE_PERMISSION_BY_CODE_KEY);
            if (hasText(roleId)) {
                keys.add(RedisKeyConstants.ROLE_PERMISSION_BY_ID_KEY + roleId);
            }
        }
        if ("tb_notify".equals(table)) {
            keys.add(hasText(userId) ? RedisKeyConstants.NOTIFY_UNREAD_KEY + userId : RedisKeyConstants.NOTIFY_UNREAD_KEY);
        }
        return keys;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
