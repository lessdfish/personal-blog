package com.bloggateway.filter;

import com.bloggateway.config.SecurityGuardProperties;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RequestGuardFilterTest {

    @Test
    void shouldBlockPathTraversal() {
        RequestGuardFilter filter = new RequestGuardFilter(properties());
        GatewayFilterChain chain = passingChain();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.method(HttpMethod.GET, URI.create("http://localhost/api/article/%2e%2e/secret"))
                .build());

        filter.filter(exchange, chain).block();

        assertGuardRejected(exchange);
        verify(chain, never()).filter(any(ServerWebExchange.class));
    }

    @Test
    void shouldBlockOversizedQuery() {
        RequestGuardFilter filter = new RequestGuardFilter(properties());
        GatewayFilterChain chain = passingChain();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/article/page")
                .queryParam("keyword", "a".repeat(33))
                .build());

        filter.filter(exchange, chain).block();

        assertGuardRejected(exchange);
        verify(chain, never()).filter(any(ServerWebExchange.class));
    }

    @Test
    void shouldBlockOversizedHeaderValue() {
        RequestGuardFilter filter = new RequestGuardFilter(properties());
        GatewayFilterChain chain = passingChain();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/article/page")
                .header("X-Trace-Input", "a".repeat(17))
                .build());

        filter.filter(exchange, chain).block();

        assertGuardRejected(exchange);
        verify(chain, never()).filter(any(ServerWebExchange.class));
    }

    @Test
    void shouldBlockOversizedBodyByContentLength() {
        RequestGuardFilter filter = new RequestGuardFilter(properties());
        GatewayFilterChain chain = passingChain();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/comment")
                .header(HttpHeaders.CONTENT_LENGTH, "11")
                .build());

        filter.filter(exchange, chain).block();

        assertGuardRejected(exchange);
        verify(chain, never()).filter(any(ServerWebExchange.class));
    }

    @Test
    void shouldAllowNormalRequest() {
        RequestGuardFilter filter = new RequestGuardFilter(properties());
        GatewayFilterChain chain = passingChain();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/article/page")
                .queryParam("pageNum", "1")
                .queryParam("pageSize", "5")
                .header("X-Trace-Input", "ok")
                .build());

        filter.filter(exchange, chain).block();

        verify(chain).filter(any(ServerWebExchange.class));
    }

    private SecurityGuardProperties properties() {
        SecurityGuardProperties properties = new SecurityGuardProperties();
        properties.setMaxRequestBodyBytes(10);
        properties.setMaxPathLength(128);
        properties.setMaxQueryLength(32);
        properties.setMaxHeaderLength(16);
        return properties;
    }

    private GatewayFilterChain passingChain() {
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
        return chain;
    }

    private void assertGuardRejected(MockServerWebExchange exchange) {
        assertEquals(HttpStatus.OK, exchange.getResponse().getStatusCode());
        assertTrue(exchange.getResponse().getBodyAsString().block().contains("request rejected by gateway guard"));
    }
}
