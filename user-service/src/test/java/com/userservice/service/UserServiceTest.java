package com.userservice.service;

import com.userservice.dto.LoginDTO;
import com.userservice.dto.RegisterDTO;
import com.userservice.dto.UpdatePasswordDTO;
import com.userservice.entity.Role;
import com.userservice.entity.User;
import com.userservice.mapper.RoleMapper;
import com.userservice.mapper.UserMapper;
import org.junit.jupiter.api.Assertions;
import com.userservice.vo.LoginVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class UserServiceTest {
    private final UserMapper userMapper = mock(UserMapper.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final RoleMapper roleMapper = mock(RoleMapper.class);
    private final StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
    private final UserActivityService userActivityService = mock(UserActivityService.class);
    private final RolePermissionCacheService rolePermissionCacheService = mock(RolePermissionCacheService.class);
    private final UserService userService = new UserService();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "userMapper", userMapper);
        ReflectionTestUtils.setField(userService, "passwordEncoder", passwordEncoder);
        ReflectionTestUtils.setField(userService, "roleMapper", roleMapper);
        ReflectionTestUtils.setField(userService, "stringRedisTemplate", stringRedisTemplate);
        ReflectionTestUtils.setField(userService, "userActivityService", userActivityService);
        ReflectionTestUtils.setField(userService, "rolePermissionCacheService", rolePermissionCacheService);
        when(rolePermissionCacheService.getPermissionCodesByRoleId(anyLong())).thenReturn(List.of());
        when(rolePermissionCacheService.getPermissionCodesByRoleCode("USER")).thenReturn(List.of());
    }

    @Test
    void loginShouldCacheTokenAndReturnUserInfo() {
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setUsername("tomuser");
        loginDTO.setPassword("password");

        User user = new User();
        user.setId(1L);
        user.setUsername("tomuser");
        user.setPassword("encoded");
        user.setNickname("Tom");
        user.setStatus(1);
        user.setRoleId(2L);

        Role role = new Role();
        role.setId(2L);
        role.setRoleCode("USER");
        role.setRoleName("普通用户");

        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(userMapper.selectByUsername("tomuser")).thenReturn(user);
        when(roleMapper.selectById(2L)).thenReturn(role);
        when(passwordEncoder.matches("password", "encoded")).thenReturn(true);

        LoginVO loginVO = userService.login(loginDTO);

        assertNotNull(loginVO);
        assertNotNull(loginVO.getToken());
        assertEquals("tomuser", loginVO.getUser().getUsername());
        assertEquals("Tom", loginVO.getUser().getNickname());
        verify(valueOperations).set(eq("blog:user:token:1"), eq(loginVO.getToken()), eq(604800L), eq(TimeUnit.SECONDS));
        verify(userActivityService).recordActivity(1L);
    }

    @Test
    void logoutShouldDeleteTokenAndOnlineState() {
        userService.logout(3L);

        verify(stringRedisTemplate).delete("blog:user:token:3");
        verify(stringRedisTemplate).delete("blog:user:online:3");
    }

    @Test
    void validateTokenShouldReturnFalseWhenTokenMismatch() {
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("blog:user:token:9")).thenReturn("token-a");

        org.junit.jupiter.api.Assertions.assertFalse(userService.validateToken(9L, "token-b"));
        verify(userActivityService, never()).recordActivity(9L);
    }

    @Test
    void registerShouldInsertEncodedUserWithDefaultRole() {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("newUser");
        dto.setPassword("plain");
        dto.setNickname("New");
        dto.setEmail("new@test.com");
        dto.setPhone("13900000023");

        Role role = new Role();
        role.setId(3L);
        role.setRoleCode("USER");

        when(userMapper.selectByUsername("newUser")).thenReturn(null);
        when(roleMapper.selectByCode("USER")).thenReturn(role);
        when(passwordEncoder.encode("plain")).thenReturn("encoded-pass");

        userService.register(dto);

        verify(userMapper).insert(any(User.class));
    }

    @Test
    void refreshTokenShouldExpireCacheAndRecordActivity() {
        userService.refreshToken(5L);

        verify(stringRedisTemplate).expire("blog:user:token:5", 604800L, TimeUnit.SECONDS);
        verify(userActivityService).recordActivity(5L);
    }

    @Test
    void updatePasswordShouldThrowWhenOldPasswordWrong() {
        UpdatePasswordDTO dto = new UpdatePasswordDTO();
        dto.setOldPassword("wrong");
        dto.setNewPassword("newPwd");

        User user = new User();
        user.setId(8L);
        user.setPassword("encoded-old");
        when(userMapper.selectById(8L)).thenReturn(user);
        when(passwordEncoder.matches("wrong", "encoded-old")).thenReturn(false);

        assertThrows(com.blogcommon.exception.BusinessException.class,
                () -> userService.updatePassword(8L, dto));
    }

    @Test
    void getActiveUserSummaryShouldReadRedisStructures() {
        @SuppressWarnings("unchecked")
        SetOperations<String, String> setOperations = mock(SetOperations.class);
        when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.size(org.mockito.ArgumentMatchers.startsWith("blog:user:active:day:"))).thenReturn(3L);
        when(setOperations.size(org.mockito.ArgumentMatchers.startsWith("blog:user:active:week:"))).thenReturn(9L);
        when(stringRedisTemplate.keys("blog:user:online:*")).thenReturn(Set.of("blog:user:online:1", "blog:user:online:2"));

        var summary = userService.getActiveUserSummary();

        assertEquals(3L, summary.getTodayActiveUsers());
        assertEquals(9L, summary.getWeekActiveUsers());
        assertEquals(2L, summary.getOnlineUsers());
    }
}
