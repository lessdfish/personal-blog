package com.userservice.config;

/**
 * ClassName:UserContext
 * Package:com.userservice.config
 * Description:
 *
 * @Author:lyp
 * @Create:2026/3/27 - 18:47
 * @Version: v1.0
 *
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
