package com.articleservice.cache;

import com.blogcommon.cache.CacheInvalidationMessage;
import com.blogcommon.cache.CacheKeyResolver;
import com.blogcommon.constant.RedisKeyConstants;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 文章缓存键解析器：把文章、版块、互动表变更转换为文章详情和热榜缓存失效键。
 */
@Component
public class ArticleCacheKeyResolver implements CacheKeyResolver {
    private static final List<String> TABLES = List.of(
            "tb_article",
            "tb_board",
            "tb_article_like",
            "tb_article_favorite",
            "tb_comment"
    );

    @Override
    public boolean supports(String tableName) {
        return TABLES.contains(tableName);
    }

    @Override
    public Collection<String> resolveKeys(CacheInvalidationMessage message) {
        List<String> keys = new ArrayList<>();
        String primaryKey = message.getPrimaryKey();
        if ("tb_article".equals(message.getTableName()) && primaryKey != null && !primaryKey.isBlank()) {
            keys.add(RedisKeyConstants.ARTICLE_DETAIL_CACHE_KEY + primaryKey);
            keys.add(RedisKeyConstants.ARTICLE_HEAT_KEY + primaryKey);
        }
        String articleId = message.getColumns() == null ? null : message.getColumns().get("article_id");
        if (articleId != null && !articleId.isBlank()) {
            keys.add(RedisKeyConstants.ARTICLE_DETAIL_CACHE_KEY + articleId);
            keys.add(RedisKeyConstants.ARTICLE_HEAT_KEY + articleId);
        }
        keys.add(RedisKeyConstants.ARTICLE_HOT_CACHE_KEY);
        return keys;
    }
}
