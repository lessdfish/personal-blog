package com.articleservice.sentinel;

/**
 * 文章服务 Sentinel 资源名常量：集中声明 Feign、缓存预热、文章详情等受保护资源。
 */
public final class ArticleSentinelResourceNames {
    public static final String USER_BATCH_SIMPLE = "article:user:batch-simple";
    public static final String ARTICLE_DETAIL = "article:detail";
    public static final String ARTICLE_HOT = "article:hot";
    public static final String ARTICLE_CACHE_WARM = "article:cache:warm";

    private ArticleSentinelResourceNames() {
    }
}
