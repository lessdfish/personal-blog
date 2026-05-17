package com.articleservice.config;

import com.blogcommon.auth.RequestUserContext;

public class UserContext {
    /**
     * 保存当前请求的用户 id，后续业务代码可以直接读取。
     */
    public static void setUserId(Long userId) {
        RequestUserContext.setUserId(userId);
    }

    /**
     * 保存当前请求的用户角色，比如 ADMIN。
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
     * 清理当前请求的用户信息，避免线程复用时串到其他请求。
     */
    public static void clear() {
        RequestUserContext.clear();
    }
}
