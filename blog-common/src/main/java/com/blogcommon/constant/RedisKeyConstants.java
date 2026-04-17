package com.blogcommon.constant;

/**
 * Redis key constants.
 */
public final class RedisKeyConstants {

    private RedisKeyConstants() {
    }

    public static final String USER_TOKEN_KEY = "blog:user:token:";
    public static final long USER_TOKEN_EXPIRE = 7 * 24 * 60 * 60;

    public static final String NOTIFY_UNREAD_KEY = "blog:notify:unread:";
    public static final long NOTIFY_UNREAD_EXPIRE = 5 * 60;

    public static final String ARTICLE_LIKES_KEY = "blog:article:likes:";
    public static final String ARTICLE_LIKED_SET_KEY = "blog:article:liked:";
    public static final int LIKES_SYNC_THRESHOLD = 100;

    public static final String ARTICLE_VIEWS_KEY = "blog:article:views:";
    public static final String ARTICLE_HEAT_KEY = "blog:article:heat:";
    public static final String ARTICLE_HEAT_RANK_KEY = "blog:article:heat:rank";
    public static final String ARTICLE_FAVORITE_SET_KEY = "blog:article:favorite:";
    public static final String ARTICLE_VIEWED_KEY = "blog:article:viewed:";

    public static final String LOCK_ARTICLE_EDIT_KEY = "blog:lock:article:edit:";
    public static final String LOCK_ARTICLE_LIKE_KEY = "blog:lock:article:like:";
    public static final String LOCK_ARTICLE_FAVORITE_KEY = "blog:lock:article:favorite:";
    public static final long LOCK_EXPIRE = 30;

    public static final String LIMIT_COMMENT_KEY = "blog:limit:comment:";
    public static final long LIMIT_COMMENT_WINDOW = 60;
    public static final int LIMIT_COMMENT_THRESHOLD = 10;

    public static final String USER_ACTIVE_DAY_KEY = "blog:user:active:day:";
    public static final String USER_ACTIVE_WEEK_KEY = "blog:user:active:week:";
    public static final String USER_ACTIVE_RANK_KEY = "blog:user:active:rank";
    public static final String USER_LAST_ACTIVE_KEY = "blog:user:last-active:";
    public static final String USER_ONLINE_KEY = "blog:user:online:";
    public static final long USER_ONLINE_EXPIRE = 10 * 60;
    public static final String USER_SESSION_INFO_KEY = "blog:user:session:";

    public static final String ROLE_PERMISSION_BY_ID_KEY = "blog:role:permission:id:";
    public static final String ROLE_PERMISSION_BY_CODE_KEY = "blog:role:permission:code:";
    public static final long ROLE_PERMISSION_CACHE_EXPIRE = 30 * 60;
}
