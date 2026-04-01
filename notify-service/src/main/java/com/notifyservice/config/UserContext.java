package com.notifyservice.config;

import jakarta.servlet.http.HttpServletRequest;

/**
 * ClassName:UserContext
 * Package:com.notifyservice.config
 * Description:用户上下文
 *
 * @Author:lyp
 * @Create:2026/4/1
 * @Version: v1.0
 */
public class UserContext {
    private static final ThreadLocal<Long> userIdHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> roleHolder = new ThreadLocal<>();
    public static void setUserId(Long userId) {
        userIdHolder.set(userId);
    }
    public static void setRole(String role) {
        roleHolder.set(role);
    }
    public static String getRole() {
        return roleHolder.get();
    }

    public static Long getUserId() {
        return userIdHolder.get();
    }

    public static void clear() {
        userIdHolder.remove();
        roleHolder.remove();
    }
}
