package com.userservice.config;

import com.blogcommon.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * ClassName:JwtInterceptor
 * Package:com.userservice.config
 * Description:
 *
 * @Author:lyp
 * @Create:2026/3/27 - 18:47
 * @Version: v1.0
 *
 */
public class JwtInterceptor implements HandlerInterceptor {

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {

            if (!(handler instanceof HandlerMethod handlerMethod)) {
                return true;
            }
            String authHeader = request.getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("""
                    {"code":401,"message":"未登录","data":null}
                    """);
                return false;
            }

            String token = authHeader.substring(7);

            try {
                Claims claims = JwtUtil.parseToken(token);
                Long userId = claims.get("userId", Long.class);
                String role = claims.get("role",String.class);
                UserContext.setUserId(userId);
                UserContext.setRole(role);

                if(handlerMethod.hasMethodAnnotation(AdminOnly.class)){
                    if(!"ADMIN".equals(role)){
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("""
                                {"code":2003,"message":"无权限访问","data":null}
                                """);
                        return false;
                    }
                }
            } catch (Exception e) {
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("""
                    {"code":401,"message":"token无效或已过期","data":null}
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

