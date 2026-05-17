package com.bloggateway.filter;

import com.blogcommon.auth.AuthConstants;
import com.blogcommon.enums.ResultCode;
import com.blogcommon.result.Result;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Component
public class CsrfProtectionFilter implements GlobalFilter, Ordered {
    private static final Set<HttpMethod> UNSAFE_METHODS = Set.of(
            HttpMethod.POST,
            HttpMethod.PUT,
            HttpMethod.PATCH,
            HttpMethod.DELETE
    );
    private static final String CSRF_ERROR_MESSAGE = "CSRF token invalid";

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${auth.csrf.enabled:true}")
    private boolean csrfEnabled;
    @Value("${auth.csrf.header-name:X-CSRF-Token}")
    private String headerName;
    @Value("${auth.csrf.cookie-name:BLOG_CSRF_TOKEN}")
    private String cookieName;
    @Value("${auth.csrf.excluded-paths:/api/user/login,/api/user/register}")
    private String excludedPaths;

    /**
     * 网关过滤器入口：检查当前请求，符合规则后再交给后续过滤器或下游服务。
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!csrfEnabled || !UNSAFE_METHODS.contains(exchange.getRequest().getMethod())) {
            return chain.filter(exchange);
        }

        String path = exchange.getRequest().getURI().getPath();
        if (isExcluded(path) || hasBearerAuthorization(exchange) || !usesCookieAuth(exchange)) {
            return chain.filter(exchange);
        }

        String headerToken = exchange.getRequest().getHeaders().getFirst(headerName);
        String cookieToken = firstCookieValue(exchange, cookieName);
        if (StringUtils.hasText(headerToken) && headerToken.equals(cookieToken)) {
            return chain.filter(exchange);
        }

        return writeError(exchange.getResponse());
    }

    /**
     * 设置过滤器执行顺序：数字越小越早执行。
     */
    @Override
    public int getOrder() {
        return -110;
    }

    /**
     * 判断当前路径是否跳过 CSRF 校验。
     */
    private boolean isExcluded(String path) {
        return excludedPathList().stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * 返回 CSRF 放行路径列表。
     */
    private List<String> excludedPathList() {
        if (!StringUtils.hasText(excludedPaths)) {
            return List.of();
        }
        return Arrays.stream(excludedPaths.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    /**
     * 判断请求是否使用 Authorization Bearer token。
     */
    private boolean hasBearerAuthorization(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        return StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ");
    }

    /**
     * 判断请求是否使用 Cookie 登录。
     */
    private boolean usesCookieAuth(ServerWebExchange exchange) {
        return StringUtils.hasText(firstCookieValue(exchange, AuthConstants.AUTH_COOKIE_NAME))
                || StringUtils.hasText(firstCookieValue(exchange, AuthConstants.REFRESH_COOKIE_NAME));
    }

    /**
     * 读取指定 Cookie 的第一个值。
     */
    private String firstCookieValue(ServerWebExchange exchange, String name) {
        return exchange.getRequest().getCookies().getOrDefault(name, List.of())
                .stream()
                .findFirst()
                .map(HttpCookie::getValue)
                .orElse(null);
    }

    /**
     * 向客户端返回错误响应：设置状态码和统一错误内容。
     */
    private Mono<Void> writeError(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.OK);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        Result<Void> result = Result.fail(ResultCode.FORBIDDEN.getCode(), CSRF_ERROR_MESSAGE);
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsString(result).getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            bytes = ("{\"code\":" + ResultCode.FORBIDDEN.getCode() + ",\"message\":\""
                    + CSRF_ERROR_MESSAGE + "\",\"data\":null}")
                    .getBytes(StandardCharsets.UTF_8);
        }
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }
}
