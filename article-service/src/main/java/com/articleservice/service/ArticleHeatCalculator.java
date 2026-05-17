package com.articleservice.service;

import com.articleservice.entity.Article;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 统一计算文章热度：自然热度 + 人工修正，再按发布时间做时间衰减。
 */
public final class ArticleHeatCalculator {
    private static final double VIEW_WEIGHT = 1D;
    private static final double COMMENT_WEIGHT = 5D;
    private static final double LIKE_WEIGHT = 4D;
    private static final double FAVORITE_WEIGHT = 3D;
    private static final double TOP_WEIGHT = 6D;
    private static final double ESSENCE_WEIGHT = 10D;
    private static final double DEFAULT_DECAY_HALF_LIFE_HOURS = 48D;

    private ArticleHeatCalculator() {
    }

    public static double calculate(Article article) {
        return calculate(article, LocalDateTime.now());
    }

    public static double calculate(Article article, LocalDateTime now) {
        if (article == null) {
            return 0D;
        }
        double baseScore = safeInt(article.getViewCount()) * VIEW_WEIGHT
                + safeInt(article.getCommentCount()) * COMMENT_WEIGHT
                + safeInt(article.getLikeCount()) * LIKE_WEIGHT
                + safeInt(article.getFavoriteCount()) * FAVORITE_WEIGHT
                + safeInt(article.getIsTop()) * TOP_WEIGHT
                + safeInt(article.getIsEssence()) * ESSENCE_WEIGHT;
        double adjustedScore = baseScore + safeDouble(article.getHotAdjustScore());
        if (!isDecayEnabled(article)) {
            return adjustedScore;
        }
        LocalDateTime createTime = article.getCreateTime();
        if (createTime == null || now == null || !createTime.isBefore(now)) {
            return adjustedScore;
        }
        long ageHours = Math.max(Duration.between(createTime, now).toHours(), 0L);
        double decayFactor = Math.exp(-ageHours / DEFAULT_DECAY_HALF_LIFE_HOURS);
        return adjustedScore * decayFactor;
    }

    private static boolean isDecayEnabled(Article article) {
        return article.getHotDecayEnabled() == null || Integer.valueOf(1).equals(article.getHotDecayEnabled());
    }

    private static int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private static double safeDouble(Double value) {
        return value == null ? 0D : value;
    }
}
