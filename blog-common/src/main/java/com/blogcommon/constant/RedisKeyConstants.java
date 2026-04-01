package com.blogcommon.constant;

/**
 * ClassName:RedisKeyConstants
 * Package:com.blogcommon.constant
 * Description:Redis缓存Key常量
 *
 * @Author:lyp
 * @Create:2026/4/1
 * @Version: v1.0
 */
public final class RedisKeyConstants {
    
    private RedisKeyConstants() {}

    // ==================== 用户Session ====================
    /** 用户登录Token缓存 key: blog:user:token:{userId} */
    public static final String USER_TOKEN_KEY = "blog:user:token:";
    /** Token过期时间：7天 */
    public static final long USER_TOKEN_EXPIRE = 7 * 24 * 60 * 60;

    // ==================== 未读消息计数 ====================
    /** 未读消息计数 key: blog:notify:unread:{userId} */
    public static final String NOTIFY_UNREAD_KEY = "blog:notify:unread:";
    /** 未读计数缓存过期时间：5分钟 */
    public static final long NOTIFY_UNREAD_EXPIRE = 5 * 60;

    // ==================== 文章点赞 ====================
    /** 文章点赞数 key: blog:article:likes:{articleId} */
    public static final String ARTICLE_LIKES_KEY = "blog:article:likes:";
    /** 用户已点赞集合 key: blog:article:liked:{userId} */
    public static final String ARTICLE_LIKED_SET_KEY = "blog:article:liked:";
    /** 点赞数据同步到DB的阈值 */
    public static final int LIKES_SYNC_THRESHOLD = 100;

    // ==================== 文章浏览量 ====================
    /** 文章浏览量 key: blog:article:views:{articleId} */
    public static final String ARTICLE_VIEWS_KEY = "blog:article:views:";

    // ==================== 分布式锁 ====================
    /** 文章编辑锁 key: blog:lock:article:edit:{articleId} */
    public static final String LOCK_ARTICLE_EDIT_KEY = "blog:lock:article:edit:";
    /** 锁过期时间：30秒 */
    public static final long LOCK_EXPIRE = 30;

    // ==================== 限流 ====================
    /** 评论限流 key: blog:limit:comment:{userId} */
    public static final String LIMIT_COMMENT_KEY = "blog:limit:comment:";
    /** 评论限流窗口：60秒 */
    public static final long LIMIT_COMMENT_WINDOW = 60;
    /** 评论限流阈值：每分钟最多10条 */
    public static final int LIMIT_COMMENT_THRESHOLD = 10;
}
