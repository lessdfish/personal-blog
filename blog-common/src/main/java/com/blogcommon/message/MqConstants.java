package com.blogcommon.message;

/**
 * ClassName:MqConstants
 * Package:com.blogcommon.message
 * Description:
 *
 * @Author:lyp
 * @Create:2026/3/30 - 23:55
 * @Version: v1.0
 *
 */
public final class MqConstants {
    public static final String COMMENT_NOTIFY_EXCHANGE = "blog.comment.exchange";
    public static final String COMMENT_NOTIFY_QUEUE = "blog.notify.queue";
    public static final String COMMENT_NOTIFY_ROUTING_KEY = "comment.notify";
    public static final String ARTICLE_INTERACTION_NOTIFY_EXCHANGE = "blog.article.interaction.exchange";
    public static final String ARTICLE_INTERACTION_NOTIFY_QUEUE = "blog.notify.interaction.queue";
    public static final String ARTICLE_INTERACTION_NOTIFY_ROUTING_KEY = "article.interaction.notify";

    private MqConstants() {
    }
}
