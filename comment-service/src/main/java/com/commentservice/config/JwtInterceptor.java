package com.commentservice.config;

import com.blogcommon.auth.JwtAuthSupport;
import com.blogcommon.auth.JwtUserInfo;
import com.blogcommon.auth.TokenSessionValidator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {
    private final TokenSessionValidator tokenSessionValidator;

    public JwtInterceptor(TokenSessionValidator tokenSessionValidator) {
        this.tokenSessionValidator = tokenSessionValidator;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        JwtUserInfo userInfo = JwtAuthSupport.parseRequiredUser(
                request,
                response,
                2001,
                "未登录",
                2002,
                "token无效或已过期"
        );
        if (userInfo == null) {
            return false;
        }
        if (!tokenSessionValidator.isTokenActive(userInfo.userId(), userInfo.token())) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":2002,\"message\":\"token无效或已退出\",\"data\":null}");
            return false;
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        JwtAuthSupport.clear();
    }
}
