package com.commentservice.config;

import com.blogcommon.result.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Feign 错误解码器
 * 用于处理远程服务调用失败的情况
 */
@Configuration
public class FeignConfig {

    @Bean
    public ErrorDecoder errorDecoder() {
        return new CustomErrorDecoder();
    }

    public static class CustomErrorDecoder implements ErrorDecoder {
        private final ObjectMapper objectMapper = new ObjectMapper();

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
