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
    public static final String COMMENT_NOTIFY_DLX = "blog.comment.dlx";
    public static final String COMMENT_NOTIFY_RETRY_QUEUE = "blog.notify.queue.retry";
    public static final String COMMENT_NOTIFY_DLQ = "blog.notify.queue.dlq";
    public static final String COMMENT_NOTIFY_RETRY_ROUTING_KEY = "comment.notify.retry";
    public static final String COMMENT_NOTIFY_DLQ_ROUTING_KEY = "comment.notify.dlq";

    public static final String ARTICLE_INTERACTION_NOTIFY_EXCHANGE = "blog.article.interaction.exchange";
    public static final String ARTICLE_INTERACTION_NOTIFY_QUEUE = "blog.notify.interaction.queue";
    public static final String ARTICLE_INTERACTION_NOTIFY_ROUTING_KEY = "article.interaction.notify";
    public static final String ARTICLE_INTERACTION_NOTIFY_DLX = "blog.article.interaction.dlx";
    public static final String ARTICLE_INTERACTION_NOTIFY_RETRY_QUEUE = "blog.notify.interaction.queue.retry";
    public static final String ARTICLE_INTERACTION_NOTIFY_DLQ = "blog.notify.interaction.queue.dlq";
    public static final String ARTICLE_INTERACTION_NOTIFY_RETRY_ROUTING_KEY = "article.interaction.notify.retry";
    public static final String ARTICLE_INTERACTION_NOTIFY_DLQ_ROUTING_KEY = "article.interaction.notify.dlq";
    public static final String ARTICLE_INTERACTION_ACTION_LIKE = "LIKE";
    public static final String ARTICLE_INTERACTION_ACTION_FAVORITE = "FAVORITE";

    public static final String ARTICLE_ES_SYNC_EXCHANGE = "blog.article.es.sync.exchange";
    public static final String ARTICLE_ES_SYNC_QUEUE = "blog.article.es.sync.queue";
    public static final String ARTICLE_ES_SYNC_ROUTING_KEY = "article.es.sync";

    private MqConstants() {
    }
}
