package com.articleservice.config;

import com.blogcommon.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * ClassName:JwtInterceptor
 * Package:com.articleservice.config
 * Description:
 *
 * @Author:lyp
 * @Create:2026/3/28 - 22:54
 * @Version: v1.0
 *
 */
public class JwtInterceptor implements HandlerInterceptor {

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            String authHeader = request.getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("""
                    {"code":2001,"message":"未登录","data":null}
                    """);
                return false;
            }

            String token = authHeader.substring(7);

            try {
                Claims claims = JwtUtil.parseToken(token);
                Long userId = claims.get("userId", Long.class);
                String role = claims.get("role", String.class);

                UserContext.setUserId(userId);
                UserContext.setRole(role);
            } catch (Exception e) {
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("""
                    {"code":2002,"message":"token无效或已过期","data":null}
                    """);
                return false;
            }

            return true;
        }

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
            UserContext.clear();
        }
    }

