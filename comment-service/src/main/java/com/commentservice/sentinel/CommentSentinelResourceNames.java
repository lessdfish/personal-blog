package com.commentservice.sentinel;

/**
 * 评论服务 Sentinel 资源名常量：集中声明评论创建、文章远程查询、用户远程查询等受保护资源。
 */
public final class CommentSentinelResourceNames {
    public static final String COMMENT_CREATE = "comment:create";
    public static final String ARTICLE_SIMPLE = "comment:article:simple";
    public static final String USER_BATCH_SIMPLE = "comment:user:batch-simple";

    private CommentSentinelResourceNames() {
    }
}
