package com.notifyservice.config;

import com.blogcommon.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * ClassName:JwtInterceptor
 * Package:com.notifyservice.config
 * Description:JWT拦截器
 *
 * @Author:lyp
 * @Create:2026/4/1
 * @Version: v1.0
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            try {
                Long userId = JwtUtil.parseToken(token).get("userId", Long.class);
                UserContext.setUserId(userId);
            } catch (Exception e) {
                // token解析失败，不设置用户信息
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
