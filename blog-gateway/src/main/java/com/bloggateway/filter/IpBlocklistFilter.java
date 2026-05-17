package com.bloggateway.filter;

import com.blogcommon.enums.ResultCode;
import com.blogcommon.result.Result;
import com.bloggateway.config.IpSecurityProperties;
import com.bloggateway.service.ClientIpResolver;
import com.bloggateway.service.IpBlocklistService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class IpBlocklistFilter implements GlobalFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(IpBlocklistFilter.class);
    private static final String BLOCKED_MESSAGE = "request blocked by dynamic ip policy";

    private final IpSecurityProperties properties;
    private final ClientIpResolver clientIpResolver;
    private final IpBlocklistService ipBlocklistService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 构造 IpBlocklistFilter：注入这个类运行时需要的依赖。
     */
    public IpBlocklistFilter(IpSecurityProperties properties,
                             ClientIpResolver clientIpResolver,
                             IpBlocklistService ipBlocklistService) {
        this.properties = properties;
        this.clientIpResolver = clientIpResolver;
        this.ipBlocklistService = ipBlocklistService;
    }

    /**
     * 网关过滤器入口：检查当前请求，符合规则后再交给后续过滤器或下游服务。
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!properties.getBlocklist().isEnabled()) {
            return chain.filter(exchange);
        }
        String clientIp = clientIpResolver.resolve(exchange);
        return ipBlocklistService.isBlocked(clientIp)
                .flatMap(blocked -> {
                    if (blocked) {
                        log.warn("gateway blocked request by dynamic ip policy clientIp={} path={}",
                                clientIp,
                                exchange.getRequest().getURI().getPath());
                        return writeError(exchange.getResponse());
                    }
                    return chain.filter(exchange);
                });
    }

    /**
     * 设置过滤器执行顺序：数字越小越早执行。
     */
    @Override
    public int getOrder() {
        return -300;
    }

    /**
     * 向客户端返回错误响应：设置状态码和统一错误内容。
     */
    private Mono<Void> writeError(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        Result<Void> result = Result.fail(ResultCode.FORBIDDEN.getCode(), BLOCKED_MESSAGE);
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsString(result).getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            bytes = ("{\"code\":" + ResultCode.FORBIDDEN.getCode() + ",\"message\":\""
                    + BLOCKED_MESSAGE + "\",\"data\":null}")
                    .getBytes(StandardCharsets.UTF_8);
        }
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }
}
