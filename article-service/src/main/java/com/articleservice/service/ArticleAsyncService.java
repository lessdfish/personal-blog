package com.articleservice.service;

import com.articleservice.entity.Article;
import com.articleservice.mapper.ArticleMapper;
import com.blogcommon.constant.RedisKeyConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class ArticleAsyncService {
    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ArticleMapper articleMapper;

    /**
     * 异步预热热榜缓存：把热门文章的热度分数提前写入 Redis，后续查热榜更快。
     */
    @Async("articleTaskExecutor")
    public void warmHotRankCache(int warmLimit) {
        if (stringRedisTemplate == null) {
            return;
        }
        try {
            Long size = stringRedisTemplate.opsForZSet().zCard(RedisKeyConstants.ARTICLE_HEAT_RANK_KEY);
            if (size != null && size >= warmLimit) {
                return;
            }
            List<Article> hotArticles = articleMapper.selectHotList(warmLimit);
            for (Article article : hotArticles) {
                double heat = ArticleHeatCalculator.calculate(article);
                stringRedisTemplate.opsForZSet().add(
                        RedisKeyConstants.ARTICLE_HEAT_RANK_KEY,
                        article.getId().toString(),
                        heat
                );
            }
        } catch (Exception e) {
            log.warn("异步预热热榜缓存失败, warmLimit={}", warmLimit, e);
        }
    }

    /**
     * 异步清理热榜列表缓存：文章数据变化后，删除旧热榜，避免用户看到过期数据。
     */
    @Async("articleTaskExecutor")
    public void evictHotListCaches() {
        if (stringRedisTemplate == null) {
            return;
        }
        try {
            Set<String> hotKeys = new HashSet<>();
            stringRedisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
                try (var cursor = connection.scan(
                        org.springframework.data.redis.core.ScanOptions.scanOptions()
                                .match(RedisKeyConstants.ARTICLE_HOT_CACHE_KEY + "*")
                                .count(100)
                                .build())) {
                    while (cursor.hasNext()) {
                        hotKeys.add(new String(cursor.next()));
                    }
                }
                return null;
            });
            if (!hotKeys.isEmpty()) {
                stringRedisTemplate.delete(hotKeys);
            }
        } catch (Exception e) {
            log.warn("异步删除热榜列表缓存失败", e);
        }
    }
}
