package com.userservice.config;

import com.blogcommon.auth.JwtAuthSupport;
import com.blogcommon.auth.JwtUserInfo;
import com.blogcommon.auth.TokenSessionValidator;
import com.userservice.service.RolePermissionCacheService;
import com.userservice.service.UserActivityService;
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
    private final TokenSessionValidator tokenSessionValidator;

    public JwtInterceptor(RolePermissionCacheService rolePermissionCacheService, UserActivityService userActivityService,
                          TokenSessionValidator tokenSessionValidator) {
        this.rolePermissionCacheService = rolePermissionCacheService;
        this.userActivityService = userActivityService;
        this.tokenSessionValidator = tokenSessionValidator;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        if ("GET".equalsIgnoreCase(request.getMethod()) && request.getRequestURI().startsWith("/user/avatar/")) {
            return true;
        }
        JwtUserInfo userInfo = JwtAuthSupport.parseRequiredUser(request, response, 2004, "UNAUTHORIZED", 2005, "INVALID_TOKEN");
        if (userInfo == null) {
            return false;
        }
        if (!tokenSessionValidator.isTokenActive(userInfo.userId(), userInfo.token())) {
            writeJson(response, 2005, "INVALID_TOKEN");
            return false;
        }
        userActivityService.recordActivity(userInfo.userId());
        if (!hasRequiredRole(handlerMethod, userInfo.role()) || !hasRequiredPermission(handlerMethod, userInfo.role())) {
            writeJson(response, 3013, "FORBIDDEN");
            return false;
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        JwtAuthSupport.clear();
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
