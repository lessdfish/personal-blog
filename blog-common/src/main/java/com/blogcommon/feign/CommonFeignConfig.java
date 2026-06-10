package com.blogcommon.feign;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Logger;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;

/**
 * 通用 Feign 配置：配置 token 透传、错误解析、连接池客户端和统一超时。
 */
public class CommonFeignConfig {
    @Bean
    public RequestInterceptor tokenRelayRequestInterceptor() {
        return new FeignTokenRelayInterceptor();
    }

    @Bean
    public ErrorDecoder errorDecoder(ObjectMapper objectMapper) {
        return new RemoteResultErrorDecoder(objectMapper);
    }

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }
}
