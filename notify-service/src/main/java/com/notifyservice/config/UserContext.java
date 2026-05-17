package com.notifyservice.config;

import com.blogcommon.auth.RequestUserContext;

    /**
     * 保存当前请求的用户 id。
     */
public class UserContext {
    public static void setUserId(Long userId) {
        RequestUserContext.setUserId(userId);
    }

    /**
     * 保存当前请求的用户角色。
     */
    public static void setRole(String role) {
        RequestUserContext.setRole(role);
    }

    /**
     * 获取当前请求的用户角色。
     */
    public static String getRole() {
        return RequestUserContext.getRole();
    }

    /**
     * 获取当前请求的用户 id。
     */
    public static Long getUserId() {
        return RequestUserContext.getUserId();
    }

    /**
     * 清理当前请求保存的用户信息。
     */
    public static void clear() {
        RequestUserContext.clear();
    }
}
