package com.bloggateway.filter;

import com.bloggateway.service.ClientIpResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class GatewayRequestLogFilter implements GlobalFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(GatewayRequestLogFilter.class);
    private final ClientIpResolver clientIpResolver;

    /**
     * 构造 GatewayRequestLogFilter：注入这个类运行时需要的依赖。
     */
    public GatewayRequestLogFilter(ClientIpResolver clientIpResolver) {
        this.clientIpResolver = clientIpResolver;
    }

    /**
     * 网关过滤器入口：检查当前请求，符合规则后再交给后续过滤器或下游服务。
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long start = System.currentTimeMillis();
        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getURI().getPath();
        String clientIp = clientIpResolver.resolve(exchange);
        return chain.filter(exchange).doFinally(signalType -> {
            long cost = System.currentTimeMillis() - start;
            log.info("gateway request method={} path={} clientIp={} status={} costMs={}",
                    method,
                    path,
                    clientIp,
                    exchange.getResponse().getStatusCode(),
                    cost);
        });
    }

    /**
     * 设置过滤器执行顺序：数字越小越早执行。
     */
    @Override
    public int getOrder() {
        return -200;
    }
}
