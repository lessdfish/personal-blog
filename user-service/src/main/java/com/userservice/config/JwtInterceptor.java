package com.userservice.config;

import com.blogcommon.util.JwtUtil;
import com.userservice.service.RolePermissionCacheService;
import com.userservice.service.UserActivityService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class JwtInterceptor implements HandlerInterceptor {
    private final RolePermissionCacheService rolePermissionCacheService;
    private final UserActivityService userActivityService;

    public JwtInterceptor(RolePermissionCacheService rolePermissionCacheService, UserActivityService userActivityService) {
        this.rolePermissionCacheService = rolePermissionCacheService;
        this.userActivityService = userActivityService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeJson(response, 2004, "未登录");
            return false;
        }

        String token = authHeader.substring(7);
        try {
            Claims claims = JwtUtil.parseToken(token);
            Long userId = parseUserId(claims.get("userId"));
            String role = claims.get("role", String.class);
            UserContext.setUserId(userId);
            UserContext.setRole(role);
            userActivityService.recordActivity(userId);

            if (!hasRequiredRole(handlerMethod, role) || !hasRequiredPermission(handlerMethod, role)) {
                writeJson(response, 3013, "无权限访问");
                return false;
            }
            return true;
        } catch (Exception e) {
            writeJson(response, 2005, "token无效或已过期");
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
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

    private boolean hasRequiredRole(HandlerMethod handlerMethod, String currentRole) {
        if (handlerMethod.hasMethodAnnotation(AdminOnly.class)) {
            return "ADMIN".equals(currentRole);
        }
        RequireRole requireRole = findAnnotation(handlerMethod, RequireRole.class);
        if (requireRole == null) {
            return true;
        }
        return Arrays.asList(requireRole.value()).contains(currentRole);
    }

    private boolean hasRequiredPermission(HandlerMethod handlerMethod, String currentRole) {
        RequirePermission requirePermission = findAnnotation(handlerMethod, RequirePermission.class);
        if (requirePermission == null) {
            return true;
        }
        List<String> permissionCodes = rolePermissionCacheService.getPermissionCodesByRoleCode(currentRole);
        Set<String> ownedPermissions = new HashSet<>(permissionCodes);
        return ownedPermissions.containsAll(Arrays.asList(requirePermission.value()));
    }

    private <A extends Annotation> A findAnnotation(HandlerMethod handlerMethod, Class<A> annotationType) {
        A methodAnnotation = handlerMethod.getMethodAnnotation(annotationType);
        return methodAnnotation != null ? methodAnnotation : handlerMethod.getBeanType().getAnnotation(annotationType);
    }

    private void writeJson(HttpServletResponse response, int code, String message) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + code + ",\"message\":\"" + message + "\",\"data\":null}");
    }
}
