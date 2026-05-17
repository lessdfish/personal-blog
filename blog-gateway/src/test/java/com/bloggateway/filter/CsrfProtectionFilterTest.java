package com.bloggateway.filter;

import com.blogcommon.auth.AuthConstants;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CsrfProtectionFilterTest {

    @Test
    void unsafeCookieRequestShouldRequireMatchingToken() {
        CsrfProtectionFilter filter = newFilter();
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/comment")
                .cookie(new HttpCookie(AuthConstants.AUTH_COOKIE_NAME, "access-token"))
                .build());

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.OK, exchange.getResponse().getStatusCode());
        assertTrue(exchange.getResponse().getBodyAsString().block().contains("CSRF token invalid"));
        verify(chain, never()).filter(any(ServerWebExchange.class));
    }

    @Test
    void unsafeCookieRequestShouldPassWithMatchingToken() {
        CsrfProtectionFilter filter = newFilter();
        GatewayFilterChain chain = passingChain();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/comment")
                .cookie(new HttpCookie(AuthConstants.AUTH_COOKIE_NAME, "access-token"))
                .cookie(new HttpCookie(AuthConstants.CSRF_COOKIE_NAME, "csrf-token"))
                .header(AuthConstants.CSRF_HEADER_NAME, "csrf-token")
                .build());

        filter.filter(exchange, chain).block();

        verify(chain).filter(any(ServerWebExchange.class));
    }

    @Test
    void bearerRequestShouldSkipCsrf() {
        CsrfProtectionFilter filter = newFilter();
        GatewayFilterChain chain = passingChain();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/comment")
                .header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
                .build());

        filter.filter(exchange, chain).block();

        verify(chain).filter(any(ServerWebExchange.class));
    }

    @Test
    void loginShouldSkipCsrf() {
        CsrfProtectionFilter filter = newFilter();
        GatewayFilterChain chain = passingChain();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/user/login")
                .build());

        filter.filter(exchange, chain).block();

        verify(chain).filter(any(ServerWebExchange.class));
    }

    @Test
    void safeGetShouldSkipCsrf() {
        CsrfProtectionFilter filter = newFilter();
        GatewayFilterChain chain = passingChain();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/comment/article/1")
                .cookie(new HttpCookie(AuthConstants.AUTH_COOKIE_NAME, "access-token"))
                .build());

        filter.filter(exchange, chain).block();

        verify(chain).filter(any(ServerWebExchange.class));
    }

    private CsrfProtectionFilter newFilter() {
        CsrfProtectionFilter filter = new CsrfProtectionFilter();
        ReflectionTestUtils.setField(filter, "csrfEnabled", true);
        ReflectionTestUtils.setField(filter, "headerName", AuthConstants.CSRF_HEADER_NAME);
        ReflectionTestUtils.setField(filter, "cookieName", AuthConstants.CSRF_COOKIE_NAME);
        ReflectionTestUtils.setField(filter, "excludedPaths", "/api/user/login,/api/user/register");
        return filter;
    }

    private GatewayFilterChain passingChain() {
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
        return chain;
    }
}
