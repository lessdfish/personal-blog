package com.userservice.controller;

import com.blogcommon.auth.AuthConstants;
import com.blogcommon.result.Result;
import com.blogcommon.util.JwtUtil;
import com.userservice.config.AvatarUploadProperties;
import com.userservice.dto.LoginDTO;
import com.userservice.service.UserService;
import com.userservice.vo.LoginUserVO;
import com.userservice.vo.LoginVO;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserTestControllerCookieTest {

    @Test
    void loginShouldSetSecureHttpOnlySameSiteCookies() {
        UserService userService = mock(UserService.class);
        UserTestController controller = newController(userService, true, "Lax");
        LoginDTO dto = new LoginDTO();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(userService.login(dto)).thenReturn(loginVO());

        Result<LoginVO> result = controller.login(dto, response, request);

        List<String> cookies = List.copyOf(response.getHeaders(HttpHeaders.SET_COOKIE));
        assertTrue(cookies.stream().anyMatch(cookie -> cookie.contains(AuthConstants.AUTH_COOKIE_NAME + "=access-token")
                && cookie.contains("Path=/")
                && cookie.contains("Max-Age=" + AuthConstants.AUTH_COOKIE_MAX_AGE)
                && cookie.contains("Secure")
                && cookie.contains("HttpOnly")
                && cookie.contains("SameSite=Lax")));
        assertTrue(cookies.stream().anyMatch(cookie -> cookie.contains(AuthConstants.REFRESH_COOKIE_NAME + "=refresh-token")
                && cookie.contains("Path=" + AuthConstants.REFRESH_COOKIE_PATH)
                && cookie.contains("Max-Age=" + AuthConstants.REFRESH_COOKIE_MAX_AGE)
                && cookie.contains("Secure")
                && cookie.contains("HttpOnly")
                && cookie.contains("SameSite=Lax")));
        assertTrue(cookies.stream().anyMatch(cookie -> cookie.contains(AuthConstants.CSRF_COOKIE_NAME + "=")
                && cookie.contains("Path=/")
                && cookie.contains("Max-Age=" + AuthConstants.CSRF_COOKIE_MAX_AGE)
                && cookie.contains("Secure")
                && !cookie.contains("HttpOnly")
                && cookie.contains("SameSite=Lax")));
        assertNull(result.getData().getToken());
        assertNull(result.getData().getRefreshToken());
        verify(userService).recordSessionInfo("tomuser", request);
    }

    @Test
    void loginShouldAllowDisablingSecureForLocalHttp() {
        UserService userService = mock(UserService.class);
        UserTestController controller = newController(userService, false, "Strict");
        LoginDTO dto = new LoginDTO();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(userService.login(dto)).thenReturn(loginVO());

        controller.login(dto, response, new MockHttpServletRequest());

        List<String> cookies = List.copyOf(response.getHeaders(HttpHeaders.SET_COOKIE));
        assertTrue(cookies.stream().allMatch(cookie -> cookie.contains("SameSite=Strict")));
        assertFalse(cookies.stream().anyMatch(cookie -> cookie.contains("Secure")));
    }

    @Test
    void sameSiteNoneShouldForceSecureCookie() {
        UserService userService = mock(UserService.class);
        UserTestController controller = newController(userService, false, "None");
        LoginDTO dto = new LoginDTO();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(userService.login(dto)).thenReturn(loginVO());

        controller.login(dto, response, new MockHttpServletRequest());

        List<String> cookies = List.copyOf(response.getHeaders(HttpHeaders.SET_COOKIE));
        assertTrue(cookies.stream().allMatch(cookie -> cookie.contains("SameSite=None") && cookie.contains("Secure")));
    }

    private UserTestController newController(UserService userService, boolean secure, String sameSite) {
        UserTestController controller = new UserTestController();
        ReflectionTestUtils.setField(controller, "userService", userService);
        ReflectionTestUtils.setField(controller, "jwtUtil", mock(JwtUtil.class));
        ReflectionTestUtils.setField(controller, "cookieSecure", secure);
        ReflectionTestUtils.setField(controller, "cookieSameSite", sameSite);
        AvatarUploadProperties avatarUploadProperties = new AvatarUploadProperties();
        avatarUploadProperties.setUploadDir("target/avatar-test");
        ReflectionTestUtils.setField(controller, "avatarUploadProperties", avatarUploadProperties);
        return controller;
    }

    private LoginVO loginVO() {
        LoginUserVO user = new LoginUserVO();
        user.setUsername("tomuser");

        LoginVO loginVO = new LoginVO();
        loginVO.setUser(user);
        loginVO.setToken("access-token");
        loginVO.setRefreshToken("refresh-token");
        return loginVO;
    }
}
