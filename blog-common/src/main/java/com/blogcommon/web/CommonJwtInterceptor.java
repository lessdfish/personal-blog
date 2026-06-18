package com.blogcommon.web;

import com.blogcommon.auth.JwtRequestAuthenticator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public class CommonJwtInterceptor implements HandlerInterceptor {
    private final JwtRequestAuthenticator jwtRequestAuthenticator;

    public CommonJwtInterceptor(JwtRequestAuthenticator jwtRequestAuthenticator) {
        this.jwtRequestAuthenticator = jwtRequestAuthenticator;
    }

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

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        jwtRequestAuthenticator.clear();
    }
}
