package com.notifyservice.config;

import com.blogcommon.auth.JwtRequestAuthenticator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {
    private final JwtRequestAuthenticator jwtRequestAuthenticator;

    /**
     * 构造 JwtInterceptor：注入这个类运行时需要的依赖。
     */
    public JwtInterceptor(JwtRequestAuthenticator jwtRequestAuthenticator) {
        this.jwtRequestAuthenticator = jwtRequestAuthenticator;
    }

    /**
     * 请求进入接口前执行：校验登录信息，并把用户身份放入当前请求上下文。
     */
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

    /**
     * 请求结束后执行：清理当前线程里的用户身份信息，避免影响下一次请求。
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        jwtRequestAuthenticator.clear();
    }
}
