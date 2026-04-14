package com.blogcommon.auth;

public final class AuthConstants {
    private AuthConstants() {
    }

    public static final String AUTH_COOKIE_NAME = "BLOG_TOKEN";
    public static final int AUTH_COOKIE_MAX_AGE = 7 * 24 * 60 * 60;
}
