package com.bloggateway.filter;

import com.bloggateway.config.IpSecurityProperties;
import com.bloggateway.service.ClientIpResolver;
import com.bloggateway.service.IpBlocklistService;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IpBlocklistFilterTest {
    @Test
    void shouldBlockRedisListedIp() {
        IpSecurityProperties properties = properties();
        ClientIpResolver resolver = new ClientIpResolver(properties);
        IpBlocklistService blocklistService = mock(IpBlocklistService.class);
        GatewayFilterChain chain = passingChain();
        MockServerWebExchange exchange = trustedProxyExchange("203.0.113.7");
        when(blocklistService.isBlocked("203.0.113.7")).thenReturn(Mono.just(true));

        new IpBlocklistFilter(properties, resolver, blocklistService).filter(exchange, chain).block();

        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        assertTrue(exchange.getResponse().getBodyAsString().block().contains("request blocked by dynamic ip policy"));
        verify(chain, never()).filter(any(ServerWebExchange.class));
    }

    @Test
    void shouldUseRemoteIpWhenProxyIsUntrusted() {
        IpSecurityProperties properties = properties();
        ClientIpResolver resolver = new ClientIpResolver(properties);
        IpBlocklistService blocklistService = mock(IpBlocklistService.class);
        GatewayFilterChain chain = passingChain();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/article/page")
                .remoteAddress(new InetSocketAddress("198.51.100.10", 12345))
                .header("X-Forwarded-For", "203.0.113.7")
                .build());
        when(blocklistService.isBlocked("198.51.100.10")).thenReturn(Mono.just(false));

        new IpBlocklistFilter(properties, resolver, blocklistService).filter(exchange, chain).block();

        verify(blocklistService).isBlocked("198.51.100.10");
        verify(chain).filter(any(ServerWebExchange.class));
    }

    private MockServerWebExchange trustedProxyExchange(String forwardedIp) {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/api/article/page")
                .remoteAddress(new InetSocketAddress("10.0.0.1", 12345))
                .header("X-Forwarded-For", forwardedIp)
                .build());
    }

    private IpSecurityProperties properties() {
        IpSecurityProperties properties = new IpSecurityProperties();
        properties.setTrustedProxies(List.of("10.0.0.1"));
        return properties;
    }

    private GatewayFilterChain passingChain() {
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
        return chain;
    }
}
