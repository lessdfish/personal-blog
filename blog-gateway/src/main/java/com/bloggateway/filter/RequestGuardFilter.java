package com.bloggateway.filter;

import com.blogcommon.enums.ResultCode;
import com.blogcommon.result.Result;
import com.bloggateway.config.SecurityGuardProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriUtils;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class RequestGuardFilter implements GlobalFilter, Ordered {
    private static final String GUARD_ERROR_MESSAGE = "request rejected by gateway guard";

    private final SecurityGuardProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 构造 RequestGuardFilter：注入这个类运行时需要的依赖。
     */
    public RequestGuardFilter(SecurityGuardProperties properties) {
        this.properties = properties;
    }

    /**
     * 网关过滤器入口：检查当前请求，符合规则后再交给后续过滤器或下游服务。
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!properties.isEnabled()) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        if (isBodyTooLarge(request)
                || isPathInvalid(request)
                || isQueryInvalid(request)
                || hasOversizedOrInvalidHeader(request)) {
            return writeError(exchange.getResponse());
        }

        return chain.filter(exchange);
    }

    /**
     * 设置过滤器执行顺序：数字越小越早执行。
     */
    @Override
    public int getOrder() {
        return -200;
    }

    /**
     * 判断请求体是否超过允许大小。
     */
    private boolean isBodyTooLarge(ServerHttpRequest request) {
        long contentLength = request.getHeaders().getContentLength();
        return contentLength > properties.getMaxRequestBodyBytes();
    }

    /**
     * 判断请求路径是否异常或过长。
     */
    private boolean isPathInvalid(ServerHttpRequest request) {
        String rawPath = request.getURI().getRawPath();
        if (!StringUtils.hasText(rawPath)) {
            return false;
        }
        if (rawPath.length() > properties.getMaxPathLength() || containsControlCharacter(rawPath)) {
            return true;
        }
        String decodedPath;
        try {
            decodedPath = UriUtils.decode(rawPath, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return true;
        }
        return containsControlCharacter(decodedPath) || containsPathTraversal(decodedPath);
    }

    /**
     * 判断查询字符串是否异常或过长。
     */
    private boolean isQueryInvalid(ServerHttpRequest request) {
        String rawQuery = request.getURI().getRawQuery();
        return rawQuery != null
                && (rawQuery.length() > properties.getMaxQueryLength() || containsControlCharacter(rawQuery));
    }

    /**
     * 判断请求头是否过长或包含非法控制字符。
     */
    private boolean hasOversizedOrInvalidHeader(ServerHttpRequest request) {
        return request.getHeaders().values().stream()
                .flatMap(values -> values.stream())
                .anyMatch(value -> value.length() > properties.getMaxHeaderLength()
                        || containsControlCharacter(value));
    }

    /**
     * 检查路径是否包含目录穿越特征。
     */
    private boolean containsPathTraversal(String path) {
        String normalized = path.replace('\\', '/');
        return normalized.equals("..")
                || normalized.startsWith("../")
                || normalized.contains("/../")
                || normalized.endsWith("/..");
    }

    /**
     * 检查字符串是否包含控制字符。
     */
    private boolean containsControlCharacter(String value) {
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch < 0x20 || ch == 0x7f) {
                return true;
            }
        }
        return false;
    }

    /**
     * 向客户端返回错误响应：设置状态码和统一错误内容。
     */
    private Mono<Void> writeError(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.OK);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        Result<Void> result = Result.fail(ResultCode.FORBIDDEN.getCode(), GUARD_ERROR_MESSAGE);
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsString(result).getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            bytes = ("{\"code\":" + ResultCode.FORBIDDEN.getCode() + ",\"message\":\""
                    + GUARD_ERROR_MESSAGE + "\",\"data\":null}")
                    .getBytes(StandardCharsets.UTF_8);
        }
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }
}
