package com.blogcommon.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtAuthSupportTest {
    private static final SecretKey KEY =
            Keys.hmacShaKeyFor("blog-cloud-secret-key-blog-cloud-secret-key".getBytes(StandardCharsets.UTF_8));

    @AfterEach
    void tearDown() {
        JwtAuthSupport.clear();
    }

    @Test
    void parseRequiredUserShouldBindContextWhenTokenValid() throws Exception {
        String token = com.blogcommon.util.JwtUtil.createToken(7L, "tester", "USER");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        JwtUserInfo userInfo = JwtAuthSupport.parseRequiredUser(
                request,
                response,
                401,
                "UNAUTHORIZED",
                2002,
                "INVALID_TOKEN"
        );

        assertNotNull(userInfo);
        assertEquals(7L, userInfo.userId());
        assertEquals("USER", userInfo.role());
        assertEquals("tester", userInfo.username());
        assertEquals(7L, RequestUserContext.getUserId());
        assertEquals("USER", RequestUserContext.getRole());
    }

    @Test
    void parseRequiredUserShouldWriteUnauthorizedWhenHeaderMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        JwtUserInfo userInfo = JwtAuthSupport.parseRequiredUser(
                request,
                response,
                401,
                "UNAUTHORIZED",
                2002,
                "INVALID_TOKEN"
        );

        assertNull(userInfo);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"code\":401"));
        assertTrue(response.getContentAsString().contains("UNAUTHORIZED"));
    }

    @Test
    void parseRequiredUserShouldWriteInvalidWhenTokenBroken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer broken-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        JwtUserInfo userInfo = JwtAuthSupport.parseRequiredUser(
                request,
                response,
                401,
                "UNAUTHORIZED",
                2002,
                "INVALID_TOKEN"
        );

        assertNull(userInfo);
        assertTrue(response.getContentAsString().contains("\"code\":2002"));
        assertTrue(response.getContentAsString().contains("INVALID_TOKEN"));
    }

    @Test
    void parseOptionalUserShouldReturnNullWhenHeaderMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        JwtUserInfo userInfo = JwtAuthSupport.parseOptionalUser(request);

        assertNull(userInfo);
        assertNull(RequestUserContext.getUserId());
    }

    @Test
    void parseOptionalUserShouldReturnNullWhenTokenBroken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer broken-token");

        JwtUserInfo userInfo = JwtAuthSupport.parseOptionalUser(request);

        assertNull(userInfo);
        assertNull(RequestUserContext.getUserId());
    }

    @Test
    void parseOptionalUserShouldBindContextWhenTokenValid() {
        String token = com.blogcommon.util.JwtUtil.createToken(8L, "optionalUser", "USER");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);

        JwtUserInfo userInfo = JwtAuthSupport.parseOptionalUser(request);

        assertNotNull(userInfo);
        assertEquals(8L, userInfo.userId());
        assertEquals("USER", RequestUserContext.getRole());
    }

    @Test
    void parseRequiredUserShouldSupportHttpOnlyCookieToken() throws Exception {
        String token = com.blogcommon.util.JwtUtil.createToken(18L, "cookie-user", "USER");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(AuthConstants.AUTH_COOKIE_NAME, token));
        MockHttpServletResponse response = new MockHttpServletResponse();

        JwtUserInfo userInfo = JwtAuthSupport.parseRequiredUser(
                request,
                response,
                401,
                "UNAUTHORIZED",
                2002,
                "INVALID_TOKEN"
        );

        assertNotNull(userInfo);
        assertEquals(18L, userInfo.userId());
        assertEquals("cookie-user", userInfo.username());
    }

    @Test
    void parseRequiredUserShouldSupportIntegerUserIdClaim() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + buildTokenWithUserId(12, "int-user", "ADMIN"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        JwtUserInfo userInfo = JwtAuthSupport.parseRequiredUser(
                request,
                response,
                401,
                "UNAUTHORIZED",
                2002,
                "INVALID_TOKEN"
        );

        assertNotNull(userInfo);
        assertEquals(12L, userInfo.userId());
        assertEquals("ADMIN", userInfo.role());
    }

    @Test
    void parseRequiredUserShouldSupportStringUserIdClaim() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + buildTokenWithUserId("15", "string-user", "USER"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        JwtUserInfo userInfo = JwtAuthSupport.parseRequiredUser(
                request,
                response,
                401,
                "UNAUTHORIZED",
                2002,
                "INVALID_TOKEN"
        );

        assertNotNull(userInfo);
        assertEquals(15L, userInfo.userId());
        assertEquals("string-user", userInfo.username());
    }

    @Test
    void parseRequiredUserShouldRejectMissingUserIdClaim() throws Exception {
        long now = System.currentTimeMillis();
        String token = Jwts.builder()
                .subject("missing-user-id")
                .claim("role", "USER")
                .issuedAt(new Date(now))
                .expiration(new Date(now + 60000))
                .signWith(KEY)
                .compact();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        JwtUserInfo userInfo = JwtAuthSupport.parseRequiredUser(
                request,
                response,
                401,
                "UNAUTHORIZED",
                2002,
                "INVALID_TOKEN"
        );

        assertNull(userInfo);
        assertTrue(response.getContentAsString().contains("\"code\":2002"));
    }

    @Test
    void clearShouldRemoveThreadLocalContext() {
        RequestUserContext.setUserId(99L);
        RequestUserContext.setRole("ADMIN");

        JwtAuthSupport.clear();

        assertNull(RequestUserContext.getUserId());
        assertNull(RequestUserContext.getRole());
    }

    private String buildTokenWithUserId(Object userId, String username, String role) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(new Date(now))
                .expiration(new Date(now + 60000))
                .signWith(KEY)
                .compact();
    }
}
