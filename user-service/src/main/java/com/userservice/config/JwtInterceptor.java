package com.userservice.config;

import com.blogcommon.auth.JwtRequestAuthenticator;
import com.blogcommon.auth.JwtUserInfo;
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
    private final JwtRequestAuthenticator jwtRequestAuthenticator;

    /**
     * 构造 JwtInterceptor：注入这个类运行时需要的依赖。
     */
    public JwtInterceptor(RolePermissionCacheService rolePermissionCacheService, UserActivityService userActivityService,
                          JwtRequestAuthenticator jwtRequestAuthenticator) {
        this.rolePermissionCacheService = rolePermissionCacheService;
        this.userActivityService = userActivityService;
        this.jwtRequestAuthenticator = jwtRequestAuthenticator;
    }

    /**
     * 请求进入接口前执行：校验登录信息，并把用户身份放入当前请求上下文。
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        if ("GET".equalsIgnoreCase(request.getMethod()) && request.getRequestURI().startsWith("/user/avatar/")) {
            return true;
        }
        JwtUserInfo userInfo = jwtRequestAuthenticator.authenticate(
                request,
                response,
                2004,
                "UNAUTHORIZED",
                2005,
                "INVALID_TOKEN",
                "INVALID_TOKEN"
        );
        if (userInfo == null) {
            return false;
        }
        userActivityService.recordActivity(userInfo.userId());
        if (!hasRequiredRole(handlerMethod, userInfo.role()) || !hasRequiredPermission(handlerMethod, userInfo.role())) {
            writeJson(response, 3013, "FORBIDDEN");
            return false;
        }
        return true;
    }

    /**
     * 请求结束后执行：清理当前线程里的用户身份信息，避免影响下一次请求。
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        jwtRequestAuthenticator.clear();
    }

    /**
     * 配置 hasRequiredRole：为当前服务准备运行时需要的组件或参数。
     */
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

    /**
     * 配置 hasRequiredPermission：为当前服务准备运行时需要的组件或参数。
     */
    private boolean hasRequiredPermission(HandlerMethod handlerMethod, String currentRole) {
        RequirePermission requirePermission = findAnnotation(handlerMethod, RequirePermission.class);
        if (requirePermission == null) {
            return true;
        }
        List<String> permissionCodes = rolePermissionCacheService.getPermissionCodesByRoleCode(currentRole);
        Set<String> ownedPermissions = new HashSet<>(permissionCodes);
        return ownedPermissions.containsAll(Arrays.asList(requirePermission.value()));
    }

    /**
     * 配置 findAnnotation：为当前服务准备运行时需要的组件或参数。
     */
    private <A extends Annotation> A findAnnotation(HandlerMethod handlerMethod, Class<A> annotationType) {
        A methodAnnotation = handlerMethod.getMethodAnnotation(annotationType);
        return methodAnnotation != null ? methodAnnotation : handlerMethod.getBeanType().getAnnotation(annotationType);
    }

    /**
     * 向响应中写入 JSON 错误信息：用于拦截器提前结束请求。
     */
    private void writeJson(HttpServletResponse response, int code, String message) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + code + ",\"message\":\"" + message + "\",\"data\":null}");
    }
}
