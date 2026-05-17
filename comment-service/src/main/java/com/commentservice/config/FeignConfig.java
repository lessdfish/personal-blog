package com.commentservice.config;

import com.blogcommon.auth.AuthConstants;
import com.blogcommon.result.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.RequestInterceptor;
import feign.Response;
import feign.codec.ErrorDecoder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Cookie;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

    /**
     * 创建 Feign 请求拦截器：调用其他服务时继续传递当前用户的登录凭证。
     */

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

    /**
     * 创建 Feign 错误解析器：把远程服务返回的错误转成当前服务能处理的异常。
     */
    @Bean
    public ErrorDecoder errorDecoder() {
        return new CustomErrorDecoder();
    }

    public static class CustomErrorDecoder implements ErrorDecoder {
        private final ObjectMapper objectMapper = new ObjectMapper();

        /**
         * 解析 Feign 调用失败响应：根据 HTTP 状态码决定抛出什么异常。
         */
        @Override
        public Exception decode(String methodKey, Response response) {
            try {
                // 尝试读取错误响应体
                if (response.body() != null) {
                    String body = new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
                    
                    // 尝试解析为 Result 对象
                    try {
                        Result<?> result = objectMapper.readValue(body, Result.class);
                        return new RuntimeException("远程服务错误: " + result.getMessage());
                    } catch (Exception e) {
                        // 无法解析为 Result，返回原始错误
                        return new RuntimeException("远程服务错误: " + body);
                    }
                }
            } catch (IOException e) {
                // 忽略
            }
            
            return new RuntimeException("远程服务调用失败: " + response.status());
        }
    }
}
