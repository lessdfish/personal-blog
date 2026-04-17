package com.articleservice.config;

import com.blogcommon.auth.AuthConstants;
import feign.RequestInterceptor;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignConfig {
    @Bean
    public RequestInterceptor tokenRelayRequestInterceptor() {
        return template -> {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return;
            }
            HttpServletRequest request = attributes.getRequest();
            if (request == null) {
                return;
            }
            String authorization = request.getHeader("Authorization");
            if (authorization != null && !authorization.isBlank()) {
                template.header("Authorization", authorization);
                return;
            }
            Cookie[] cookies = request.getCookies();
            if (cookies == null) {
                return;
            }
            for (Cookie cookie : cookies) {
                if (AuthConstants.AUTH_COOKIE_NAME.equals(cookie.getName())
                        && cookie.getValue() != null
                        && !cookie.getValue().isBlank()) {
                    template.header("Authorization", "Bearer " + cookie.getValue());
                    return;
                }
            }
        };
    }
}
