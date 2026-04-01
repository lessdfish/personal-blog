package com.commentservice.config;

import com.blogcommon.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * ClassName:JwtInterceptor
 * Package:com.commentservice.config
 * Description:JWT拦截器，用于解析token并设置用户上下文
 *
 * @Author:lyp
 * @Create:2026/3/28 - 22:54
 * @Version: v1.0
 */
public class JwtInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authHeader = request.getHeader("Authorization");
        
        // 调试日志
        System.out.println("[JwtInterceptor] Authorization header: " + authHeader);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("[JwtInterceptor] 未登录：Authorization header 为空或格式错误");
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("""
                {"code":2001,"message":"未登录","data":null}
                """);
            return false;
        }

        String token = authHeader.substring(7);  // 去掉 "Bearer " 前缀
        System.out.println("[JwtInterceptor] Token: " + token);

        try {
            Claims claims = JwtUtil.parseToken(token);
            
            // 修复：处理 Integer 到 Long 的转换问题
            Object userIdObj = claims.get("userId");
            Long userId = null;
            if (userIdObj instanceof Integer) {
                userId = ((Integer) userIdObj).longValue();
            } else if (userIdObj instanceof Long) {
                userId = (Long) userIdObj;
            } else if (userIdObj != null) {
                userId = Long.parseLong(userIdObj.toString());
            }
            
            String role = claims.get("role", String.class);
            
            System.out.println("[JwtInterceptor] 解析成功 - userId: " + userId + ", role: " + role);

            if (userId == null) {
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("""
                    {"code":2002,"message":"token无效：userId为空","data":null}
                    """);
                return false;
            }

            UserContext.setUserId(userId);
            UserContext.setRole(role);
            
        } catch (Exception e) {
            System.out.println("[JwtInterceptor] Token解析异常: " + e.getMessage());
            e.printStackTrace();
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

