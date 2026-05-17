package com.blogcommon.auth;

import com.blogcommon.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthSupport {
    private final JwtUtil jwtUtil;

    public JwtAuthSupport(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    public JwtUserInfo parseRequiredUser(HttpServletRequest request, HttpServletResponse response,
                                         int unauthorizedCode, String unauthorizedMessage,
                                         int invalidCode, String invalidMessage) throws IOException {
        String token = extractToken(request);
        if (token == null) {
            writeJson(response, unauthorizedCode, unauthorizedMessage);
            return null;
        }
        try {
            JwtUserInfo userInfo = parseToken(token);
            bind(userInfo);
            return userInfo;
        } catch (Exception e) {
            writeJson(response, invalidCode, invalidMessage);
            return null;
        }
    }

    public JwtUserInfo parseOptionalUser(HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null) {
            return null;
        }
        try {
            JwtUserInfo userInfo = parseToken(token);
            bind(userInfo);
            return userInfo;
        } catch (Exception e) {
            return null;
        }
    }

    public void clear() {
        RequestUserContext.clear();
    }

    private JwtUserInfo parseToken(String token) {
        Claims claims = jwtUtil.parseToken(token);
        Long userId = parseUserId(claims.get("userId"));
        String role = claims.get("role", String.class);
        String username = claims.getSubject();
        if (userId == null) {
            throw new IllegalArgumentException("userId is null");
        }
        return new JwtUserInfo(userId, role, username, token);
    }

    private void bind(JwtUserInfo userInfo) {
        RequestUserContext.setUserId(userInfo.userId());
        RequestUserContext.setRole(userInfo.role());
        RequestUserContext.setUsername(userInfo.username());
    }

    public static String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (AuthConstants.AUTH_COOKIE_NAME.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private Long parseUserId(Object userIdObj) {
        if (userIdObj == null) {
            return null;
        }
        if (userIdObj instanceof Long value) {
            return value;
        }
        if (userIdObj instanceof Integer value) {
            return value.longValue();
        }
        return Long.parseLong(userIdObj.toString());
    }

    private void writeJson(HttpServletResponse response, int code, String message) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + code + ",\"message\":\"" + message + "\",\"data\":null}");
    }
}
