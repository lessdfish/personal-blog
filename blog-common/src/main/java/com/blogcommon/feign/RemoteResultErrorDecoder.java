package com.blogcommon.feign;

import com.blogcommon.result.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Feign 错误解析器：把远程 Result 错误或 HTTP 错误转换为统一 RemoteCallException。
 */
public class RemoteResultErrorDecoder implements ErrorDecoder {
    private final ObjectMapper objectMapper;

    public RemoteResultErrorDecoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Exception decode(String methodKey, Response response) {
        String body = readBody(response);
        String message = resolveMessage(body, response.status());
        return new RemoteCallException(methodKey, response.status(), body, message);
    }

    private String readBody(Response response) {
        if (response.body() == null) {
            return "";
        }
        try {
            return new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    private String resolveMessage(String body, int status) {
        if (body == null || body.isBlank()) {
            return "远程服务调用失败: " + status;
        }
        try {
            Result<?> result = objectMapper.readValue(body, Result.class);
            if (result.getMessage() != null && !result.getMessage().isBlank()) {
                return "远程服务错误: " + result.getMessage();
            }
        } catch (Exception ignored) {
        }
        return "远程服务错误: " + body;
    }
}
