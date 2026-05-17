package com.articleservice.config;

import com.blogcommon.auth.JwtRequestAuthenticator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {
    private final JwtRequestAuthenticator jwtRequestAuthenticator;

    /**
     * 构造 JWT 拦截器：注入 token 解析和登录状态校验所需的工具。
     */
    public JwtInterceptor(JwtRequestAuthenticator jwtRequestAuthenticator) {
        this.jwtRequestAuthenticator = jwtRequestAuthenticator;
    }

    /**
     * 请求进入接口前执行：检查用户是否登录、token 是否有效。
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        return jwtRequestAuthenticator.authenticate(
                request,
                response,
                2001,
                "未登录",
                2002,
                "token无效或已过期",
                "token无效或已退出"
        ) != null;
    }

    /**
     * 请求结束后执行：清理当前线程里保存的用户信息，避免影响下一次请求。
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        jwtRequestAuthenticator.clear();
    }
}
