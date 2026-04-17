package com.bloggateway.filter;

import com.blogcommon.auth.AuthConstants;
import com.blogcommon.enums.ResultCode;
import com.blogcommon.result.Result;
import com.blogcommon.util.JwtUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

@Component
public class GatewayAuthFilter implements GlobalFilter, Ordered {
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final List<String> WHITE_LIST = List.of(
            "/api/user/login",
            "/api/user/register",
            "/api/user/check",
            "/api/user/parse",
            "/api/user/batch/simple",
            "/api/article/page",
            "/api/article/page/normal",
            "/api/article/page/hot",
            "/api/article/detail/**",
            "/api/article/hot",
            "/api/article/simple/**",
            "/api/article/board/list",
            "/api/article/heat/**",
            "/api/article/likes/**",
            "/api/article/favorites/count/**",
            "/api/article/views/**",
            "/api/comment/article/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (isWhiteListed(path)) {
            return chain.filter(exchange);
        }

        String token = resolveToken(exchange);
        if (!StringUtils.hasText(token)) {
            return writeError(exchange.getResponse(), ResultCode.UNAUTHORIZED);
        }

        try {
            Claims claims = JwtUtil.parseToken(token);
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .header("X-User-Id", String.valueOf(claims.get("userId")))
                    .header("X-User-Role", String.valueOf(claims.get("role")))
                    .header("X-Username", String.valueOf(claims.getSubject()))
                    .build();
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } catch (Exception e) {
            return writeError(exchange.getResponse(), ResultCode.GATEWAY_AUTH_ERROR);
        }
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private boolean isWhiteListed(String path) {
        return WHITE_LIST.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }

    private String resolveToken(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return exchange.getRequest().getCookies().getOrDefault(AuthConstants.AUTH_COOKIE_NAME, List.of())
                .stream()
                .map(cookie -> cookie.getValue())
                .filter(Objects::nonNull)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }

    private Mono<Void> writeError(ServerHttpResponse response, ResultCode resultCode) {
        response.setStatusCode(HttpStatus.OK);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        Result<Void> result = Result.fail(resultCode.getCode(), resultCode.getMessage());
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsString(result).getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            bytes = ("{\"code\":" + resultCode.getCode() + ",\"message\":\"" + resultCode.getMessage() + "\",\"data\":null}")
                    .getBytes(StandardCharsets.UTF_8);
        }
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }
}
