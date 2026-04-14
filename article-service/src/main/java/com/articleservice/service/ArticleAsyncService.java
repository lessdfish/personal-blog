package com.articleservice.service;

import com.articleservice.entity.Article;
import com.articleservice.mapper.ArticleMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class ArticleAsyncService {
    private static final String ARTICLE_HOT_CACHE_KEY = "blog:article:hot:";
    private static final String ARTICLE_HEAT_RANK_KEY = "blog:article:heat:rank";

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ArticleMapper articleMapper;

    @Async("articleTaskExecutor")
    public void warmHotRankCache(int warmLimit) {
        if (stringRedisTemplate == null) {
            return;
        }
        try {
            Long size = stringRedisTemplate.opsForZSet().zCard(ARTICLE_HEAT_RANK_KEY);
            if (size != null && size >= warmLimit) {
                return;
            }
            List<Article> hotArticles = articleMapper.selectHotList(warmLimit);
            for (Article article : hotArticles) {
                double heat = calculateHeat(article);
                stringRedisTemplate.opsForZSet().add(ARTICLE_HEAT_RANK_KEY, article.getId().toString(), heat);
            }
        } catch (Exception e) {
            log.warn("异步预热热榜缓存失败, warmLimit={}", warmLimit, e);
        }
    }

    @Async("articleTaskExecutor")
    public void evictHotListCaches() {
        if (stringRedisTemplate == null) {
            return;
        }
        try {
            Set<String> hotKeys = stringRedisTemplate.keys(ARTICLE_HOT_CACHE_KEY + "*");
            if (hotKeys != null && !hotKeys.isEmpty()) {
                stringRedisTemplate.delete(hotKeys);
            }
        } catch (Exception e) {
            log.warn("异步删除热榜列表缓存失败", e);
        }
    }

    private double calculateHeat(Article article) {
        return article.getViewCount() * 1D
                + article.getCommentCount() * 5D
                + article.getLikeCount() * 4D
                + article.getFavoriteCount() * 3D
                + (article.getIsTop() == null ? 0 : article.getIsTop() * 6D)
                + (article.getIsEssence() == null ? 0 : article.getIsEssence() * 10D);
    }
}
