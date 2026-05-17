package com.blogcommon.auth;

public final class AuthConstants {
    private AuthConstants() {
    }

    public static final String AUTH_COOKIE_NAME = "BLOG_TOKEN";
    public static final int AUTH_COOKIE_MAX_AGE = 24 * 60 * 60;
    public static final String REFRESH_COOKIE_NAME = "BLOG_REFRESH_TOKEN";
    public static final String REFRESH_COOKIE_PATH = "/api/user/token/refresh";
    public static final int REFRESH_COOKIE_MAX_AGE = 7 * 24 * 60 * 60;
    public static final String CSRF_COOKIE_NAME = "BLOG_CSRF_TOKEN";
    public static final String CSRF_HEADER_NAME = "X-CSRF-Token";
    public static final int CSRF_COOKIE_MAX_AGE = AUTH_COOKIE_MAX_AGE;
}
