package com.blogcommon.auth;

import com.blogcommon.util.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtRequestAuthenticatorTest {
    private static final String SECRET = "blog-cloud-secret-key-blog-cloud-secret-key-blog-cloud-secret-key";
    private final JwtUtil jwtUtil = new JwtUtil(SECRET, 86400);
    private final JwtAuthSupport jwtAuthSupport = new JwtAuthSupport(jwtUtil);
    private final StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
    private final TokenSessionValidator tokenSessionValidator = new TokenSessionValidator();
    private final JwtRequestAuthenticator authenticator =
            new JwtRequestAuthenticator(jwtAuthSupport, tokenSessionValidator);

    JwtRequestAuthenticatorTest() {
        ReflectionTestUtils.setField(tokenSessionValidator, "stringRedisTemplate", stringRedisTemplate);
        ReflectionTestUtils.setField(tokenSessionValidator, "failOpenOnRedisError", false);
    }

    @AfterEach
    void tearDown() {
        RequestUserContext.clear();
    }

    @Test
    void authenticateShouldReturnUserWhenTokenActive() throws Exception {
        String token = jwtUtil.createToken(7L, "alice", "USER");
        MockHttpServletRequest request = requestWithToken(token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("blog:user:token:7")).thenReturn(token);

        JwtUserInfo userInfo = authenticator.authenticate(
                request, response, 2001, "未登录", 2002, "token无效或已过期", "token无效或已退出");

        assertNotNull(userInfo);
        assertEquals(7L, userInfo.userId());
        assertEquals("alice", RequestUserContext.getUsername());
    }

    @Test
    void authenticateShouldWriteInactiveTokenWhenSessionMissing() throws Exception {
        String token = jwtUtil.createToken(8L, "bob", "USER");
        MockHttpServletRequest request = requestWithToken(token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("blog:user:token:8")).thenReturn(null);

        JwtUserInfo userInfo = authenticator.authenticate(
                request, response, 2001, "未登录", 2002, "token无效或已过期", "token无效或已退出");

        assertNull(userInfo);
        assertTrue(response.getContentAsString().contains("\"code\":2002"));
        assertTrue(response.getContentAsString().contains("token无效或已退出"));
    }

    private MockHttpServletRequest requestWithToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }
}
