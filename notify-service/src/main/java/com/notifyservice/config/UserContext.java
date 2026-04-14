package com.notifyservice.config;

import com.blogcommon.auth.RequestUserContext;

public class UserContext {
    public static void setUserId(Long userId) {
        RequestUserContext.setUserId(userId);
    }

    public static void setRole(String role) {
        RequestUserContext.setRole(role);
    }

    public static String getRole() {
        return RequestUserContext.getRole();
    }

    public static Long getUserId() {
        return RequestUserContext.getUserId();
    }

    public static void clear() {
        RequestUserContext.clear();
    }
}
