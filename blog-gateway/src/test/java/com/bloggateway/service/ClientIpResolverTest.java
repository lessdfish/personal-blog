package com.bloggateway.service;

import com.bloggateway.config.IpSecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.net.InetSocketAddress;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientIpResolverTest {
    @Test
    void shouldTrustForwardedForOnlyFromTrustedProxy() {
        ClientIpResolver resolver = new ClientIpResolver(properties(List.of("10.0.0.1")));
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/article/page")
                .remoteAddress(new InetSocketAddress("10.0.0.1", 12345))
                .header("X-Forwarded-For", "203.0.113.7, 10.0.0.1")
                .build());

        assertEquals("203.0.113.7", resolver.resolve(exchange));
    }

    @Test
    void shouldIgnoreSpoofedForwardedForFromUntrustedRemote() {
        ClientIpResolver resolver = new ClientIpResolver(properties(List.of("10.0.0.1")));
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/article/page")
                .remoteAddress(new InetSocketAddress("198.51.100.9", 12345))
                .header("X-Forwarded-For", "203.0.113.7")
                .build());

        assertEquals("198.51.100.9", resolver.resolve(exchange));
    }

    @Test
    void shouldMatchTrustedCidr() {
        ClientIpResolver resolver = new ClientIpResolver(properties(List.of("10.0.0.0/8")));

        assertTrue(resolver.isTrustedProxy("10.2.3.4"));
        assertFalse(resolver.isTrustedProxy("11.2.3.4"));
    }

    @Test
    void shouldUseHostStringWhenRemoteAddressIsUnresolved() {
        ClientIpResolver resolver = new ClientIpResolver(properties(List.of("127.0.0.1")));
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/article/page")
                .remoteAddress(InetSocketAddress.createUnresolved("203.0.113.199", 12345))
                .build());

        assertEquals("203.0.113.199", resolver.resolve(exchange));
    }

    private IpSecurityProperties properties(List<String> trustedProxies) {
        IpSecurityProperties properties = new IpSecurityProperties();
        properties.setTrustedProxies(trustedProxies);
        return properties;
    }
}
