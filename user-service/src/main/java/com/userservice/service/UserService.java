package com.userservice.service;

import com.blogcommon.constant.RedisKeyConstants;
import com.blogcommon.enums.ResultCode;
import com.blogcommon.exception.BusinessException;
import com.blogcommon.logging.DbWriteAuditLogger;
import com.blogcommon.auth.RequestUserContext;
import com.blogcommon.util.JwtUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.userservice.converter.UserConverter;
import com.userservice.dto.LoginDTO;
import com.userservice.dto.RegisterDTO;
import com.userservice.dto.ResetPasswordByPhoneDTO;
import com.userservice.dto.UpdatePasswordDTO;
import com.userservice.dto.UpdateUserInfoDTO;
import com.userservice.dto.UpdateUserRoleDTO;
import com.userservice.dto.UpdateUserStatusDTO;
import com.userservice.dto.UserPageQueryDTO;
import com.userservice.entity.Role;
import com.userservice.entity.User;
import com.userservice.mapper.RoleMapper;
import com.userservice.mapper.UserMapper;
import com.userservice.vo.ActiveUserSummaryVO;
import com.userservice.vo.ActiveUserVO;
import com.userservice.vo.CurrentUserVO;
import com.userservice.vo.LoginVO;
import com.userservice.vo.LoginUserVO;
import com.userservice.vo.PageVO;
import com.userservice.vo.RolePermissionVO;
import com.userservice.vo.SessionInfoVO;
import com.userservice.vo.UserInfoVO;
import com.userservice.vo.UserSimpleVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class UserService {
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RoleMapper roleMapper;
    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserActivityService userActivityService;
    @Autowired
    private RolePermissionCacheService rolePermissionCacheService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private AvatarStorageService avatarStorageService;

    /**
     * 注册用户：校验注册信息，加密密码，然后保存新用户。
     */
    public void register(RegisterDTO registerDTO) {
        User dbUser = userMapper.selectByUsername(registerDTO.getUsername());
        if (dbUser != null) {
            throw new BusinessException(ResultCode.USERNAME_EXIST);
        }
        validateUniqueProfileFields(registerDTO.getNickname(), registerDTO.getEmail(), registerDTO.getPhone(), null);

        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setNickname(registerDTO.getNickname());
        user.setEmail(registerDTO.getEmail());
        user.setPhone(registerDTO.getPhone());
        user.setStatus(1);
        Role defaultRole = roleMapper.selectByCode("USER");
        if (defaultRole == null) {
            throw new BusinessException(ResultCode.ROLE_NULL);
        }
        user.setRoleId(defaultRole.getId());

        userMapper.insert(user);
        DbWriteAuditLogger.logInsert("tb_user", user);
    }

    /**
     * 检查字段是否可用：判断用户名、昵称、邮箱或手机号是否已被占用。
     */
    public boolean isFieldAvailable(String field, String value) {
        if (field == null || field.isBlank() || value == null || value.isBlank()) {
            throw new BusinessException(ResultCode.PARAM_NULL.getCode(), "校验字段和值不能为空");
        }
        return switch (field) {
            case "username" -> userMapper.selectByUsername(value.trim()) == null;
            case "nickname" -> userMapper.selectByNickname(value.trim()) == null;
            case "email" -> userMapper.selectByEmail(value.trim()) == null;
            case "phone" -> userMapper.selectByPhone(value.trim()) == null;
            default -> throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "不支持的校验字段");
        };
    }

    /**
     * 用户登录：校验账号密码，生成 token，并返回登录用户信息。
     */
    public LoginVO login(LoginDTO loginDTO) {
        User dbUser = userMapper.selectByUsername(loginDTO.getUsername());
        if (dbUser == null) {
            throw new BusinessException(ResultCode.USERNAME_NOT_EXIST);
        }
        Role role = roleMapper.selectById(dbUser.getRoleId());
        if (role == null) {
            throw new BusinessException(ResultCode.ROLE_NULL);
        }
        if (Integer.valueOf(0).equals(dbUser.getStatus())) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }
        if (!passwordEncoder.matches(loginDTO.getPassword(), dbUser.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }

        String token = jwtUtil.createToken(dbUser.getId(), dbUser.getUsername(), role.getRoleCode());
        String refreshToken = UUID.randomUUID().toString();
        cacheToken(dbUser.getId(), token);
        cacheRefreshToken(dbUser.getId(), refreshToken);
        userActivityService.recordActivity(dbUser.getId());

        LoginUserVO loginUserVO = UserConverter.toLoginUserVO(dbUser);
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setRefreshToken(refreshToken);
        loginVO.setUser(loginUserVO);
        return loginVO;
    }

    /**
     * 缓存访问 token：保存用户当前有效的登录凭证。
     */
    private void cacheToken(Long userId, String token) {
        if (stringRedisTemplate != null) {
            String key = RedisKeyConstants.USER_TOKEN_KEY + userId;
            stringRedisTemplate.opsForValue().set(key, token, RedisKeyConstants.USER_TOKEN_EXPIRE, TimeUnit.SECONDS);
        }
    }

    /**
     * 缓存刷新 token：保存用于续期登录的凭证。
     */
    private void cacheRefreshToken(Long userId, String refreshToken) {
        if (stringRedisTemplate != null) {
            stringRedisTemplate.opsForValue().set(
                    RedisKeyConstants.USER_REFRESH_TOKEN_KEY + userId,
                    refreshToken,
                    RedisKeyConstants.USER_REFRESH_TOKEN_EXPIRE,
                    TimeUnit.SECONDS);
            stringRedisTemplate.opsForValue().set(
                    RedisKeyConstants.REFRESH_TOKEN_LOOKUP_KEY + refreshToken,
                    userId.toString(),
                    RedisKeyConstants.USER_REFRESH_TOKEN_EXPIRE,
                    TimeUnit.SECONDS);
        }
    }

    /**
     * 校验 token 是否仍然有效：常用于网关或其他服务确认登录状态。
     */
    public boolean validateToken(Long userId, String token) {
        if (stringRedisTemplate == null) {
            return true;
        }
        String key = RedisKeyConstants.USER_TOKEN_KEY + userId;
        String cachedToken = stringRedisTemplate.opsForValue().get(key);
        return token != null && token.equals(cachedToken);
    }

    /**
     * 退出登录：清理当前用户的登录状态和 token。
     */
    public void logout(Long userId) {
        if (stringRedisTemplate != null && userId != null) {
            String key = RedisKeyConstants.USER_TOKEN_KEY + userId;
            deleteRefreshToken(userId);
            stringRedisTemplate.delete(key);
            stringRedisTemplate.delete(RedisKeyConstants.USER_ONLINE_KEY + userId);
            stringRedisTemplate.delete(RedisKeyConstants.USER_SESSION_INFO_KEY + userId);
        }
    }

    /**
     * 强制用户下线：管理员让指定用户的 token 失效。
     */
    public void kickout(Long userId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.PARAM_NULL);
        }
        requireAdmin();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        logout(userId);
    }

    /**
     * 刷新登录令牌：为用户生成新的访问 token。
     */
    public void refreshToken(Long userId) {
        if (stringRedisTemplate != null && userId != null) {
            String key = RedisKeyConstants.USER_TOKEN_KEY + userId;
            stringRedisTemplate.expire(key, RedisKeyConstants.USER_TOKEN_EXPIRE, TimeUnit.SECONDS);
            stringRedisTemplate.expire(RedisKeyConstants.USER_SESSION_INFO_KEY + userId, RedisKeyConstants.USER_TOKEN_EXPIRE, TimeUnit.SECONDS);
        }
        userActivityService.recordActivity(userId);
    }

    /**
     * 根据刷新 token 重新签发访问 token。
     */
    public LoginVO refreshAccessToken(String refreshToken) {
        if (stringRedisTemplate == null || refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        String userIdText = stringRedisTemplate.opsForValue().get(RedisKeyConstants.REFRESH_TOKEN_LOOKUP_KEY + refreshToken);
        if (userIdText == null || userIdText.isBlank()) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }
        Long userId = Long.valueOf(userIdText);
        String cachedRefreshToken = stringRedisTemplate.opsForValue().get(RedisKeyConstants.USER_REFRESH_TOKEN_KEY + userId);
        if (!refreshToken.equals(cachedRefreshToken)) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        if (Integer.valueOf(0).equals(user.getStatus())) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }
        Role role = roleMapper.selectById(user.getRoleId());
        if (role == null) {
            throw new BusinessException(ResultCode.ROLE_NULL);
        }

        stringRedisTemplate.delete(RedisKeyConstants.REFRESH_TOKEN_LOOKUP_KEY + refreshToken);
        String newAccessToken = jwtUtil.createToken(user.getId(), user.getUsername(), role.getRoleCode());
        String newRefreshToken = UUID.randomUUID().toString();
        cacheToken(user.getId(), newAccessToken);
        cacheRefreshToken(user.getId(), newRefreshToken);
        userActivityService.recordActivity(user.getId());

        LoginVO loginVO = new LoginVO();
        loginVO.setToken(newAccessToken);
        loginVO.setRefreshToken(newRefreshToken);
        loginVO.setUser(UserConverter.toLoginUserVO(user));
        return loginVO;
    }

    /**
     * 删除刷新 token：用户退出或令牌失效时调用。
     */
    private void deleteRefreshToken(Long userId) {
        String refreshToken = stringRedisTemplate.opsForValue().get(RedisKeyConstants.USER_REFRESH_TOKEN_KEY + userId);
        if (refreshToken != null && !refreshToken.isBlank()) {
            stringRedisTemplate.delete(RedisKeyConstants.REFRESH_TOKEN_LOOKUP_KEY + refreshToken);
        }
        stringRedisTemplate.delete(RedisKeyConstants.USER_REFRESH_TOKEN_KEY + userId);
    }

    /**
     * 获取 currentUserInfo：返回当前对象里保存的这个值。
     */
    public CurrentUserVO getCurrentUserInfo(Long userId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USERNAME_NOT_EXIST);
        }
        CurrentUserVO vo = UserConverter.toCurrentUserVO(user);
        vo.setSessionInfo(getSessionInfo(userId));
        return vo;
    }

    /**
     * 业务方法 recordSessionInfo：封装 UserService 中对应的核心处理流程。
     */
    public void recordSessionInfo(String username, HttpServletRequest request) {
        if (stringRedisTemplate == null || request == null || username == null || username.isBlank()) {
            return;
        }
        User user = userMapper.selectByUsername(username);
        if (user == null || user.getId() == null) {
            return;
        }
        SessionInfoVO sessionInfo = buildSessionInfo(request);
        try {
            stringRedisTemplate.opsForValue().set(
                    RedisKeyConstants.USER_SESSION_INFO_KEY + user.getId(),
                    objectMapper.writeValueAsString(sessionInfo),
                    RedisKeyConstants.USER_TOKEN_EXPIRE,
                    TimeUnit.SECONDS);
        } catch (JsonProcessingException ignored) {
        }
    }

    /**
     * 业务方法 updateCurrentUserInfo：封装 UserService 中对应的核心处理流程。
     */
    public void updateCurrentUserInfo(Long userId, UpdateUserInfoDTO updateUserInfoDTO) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        User dbUser = userMapper.selectById(userId);
        if (dbUser == null) {
            throw new BusinessException(ResultCode.USERNAME_NOT_EXIST);
        }
        validateUniqueProfileFields(
                updateUserInfoDTO.getNickname(),
                updateUserInfoDTO.getEmail(),
                updateUserInfoDTO.getPhone(),
                userId);

        User user = new User();
        user.setId(userId);
        user.setNickname(updateUserInfoDTO.getNickname());
        user.setAvatar(updateUserInfoDTO.getAvatar());
        user.setEmail(updateUserInfoDTO.getEmail());
        user.setPhone(updateUserInfoDTO.getPhone());

        int rows = userMapper.updateUserInfo(user);
        if (rows <= 0) {
            throw new BusinessException(ResultCode.USER_UPDATE_FAILED);
        }
    }

    /**
     * 业务方法 uploadAvatar：封装 UserService 中对应的核心处理流程。
     */
    public String uploadAvatar(Long userId, MultipartFile file) {
        return avatarStorageService.store(userId, file);
    }

    /**
     * 修改密码：校验旧密码后保存新密码。
     */
    public void updatePassword(Long userId, UpdatePasswordDTO updatePasswordDTO) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        User dbUser = userMapper.selectById(userId);
        if (dbUser == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }

        if (!passwordEncoder.matches(updatePasswordDTO.getOldPassword(), dbUser.getPassword())) {
            throw new BusinessException(ResultCode.OLD_PASSWORD_ERROR);
        }
        if (passwordEncoder.matches(updatePasswordDTO.getNewPassword(), dbUser.getPassword())) {
            throw new BusinessException(ResultCode.NOT_SAME);
        }

        String newEncodedPassword = passwordEncoder.encode(updatePasswordDTO.getNewPassword());
        int rows = userMapper.updatePassword(userId, newEncodedPassword);
        if (rows <= 0) {
            throw new BusinessException(ResultCode.PASSWORD_UPDATE_FAILED);
        }
    }

    /**
     * 通过手机号重置密码：校验用户和手机号后设置新密码。
     */
    public void resetPasswordByPhone(ResetPasswordByPhoneDTO dto) {
        User dbUser = userMapper.selectByUsername(dto.getUsername());
        if (dbUser == null) {
            throw new BusinessException(ResultCode.USERNAME_NOT_EXIST);
        }
        if (dbUser.getPhone() == null || !dbUser.getPhone().equals(dto.getPhone())) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST.getCode(), "手机号校验失败");
        }
        if (passwordEncoder.matches(dto.getNewPassword(), dbUser.getPassword())) {
            throw new BusinessException(ResultCode.NOT_SAME);
        }

        String newEncodedPassword = passwordEncoder.encode(dto.getNewPassword());
        int rows = userMapper.updatePassword(dbUser.getId(), newEncodedPassword);
        if (rows <= 0) {
            throw new BusinessException(ResultCode.PASSWORD_UPDATE_FAILED);
        }
        logout(dbUser.getId());
    }

    /**
     * 分页查询用户：管理员按条件查看用户列表。
     */
    public PageVO<UserInfoVO> pageUsers(UserPageQueryDTO queryDTO) {
        if (queryDTO == null) {
            throw new BusinessException(ResultCode.PARAM_NOT_NULL);
        }

        Integer pageNum = queryDTO.getPageNum();
        Integer pageSize = queryDTO.getPageSize();
        if (pageNum == null || pageNum < 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        if (pageSize == null || pageSize < 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR1);
        }

        Page<User> page = PageHelper.startPage(pageNum, pageSize);
        List<User> userList = userMapper.selectUserListByCondition(queryDTO.getUsername(), queryDTO.getStatus());
        List<Long> roleIds = userList.stream()
                .map(User::getRoleId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Role> roleMap = roleIds.isEmpty()
                ? Map.of()
                : roleMapper.selectByIds(roleIds).stream()
                .collect(Collectors.toMap(Role::getId, r -> r));
        List<UserInfoVO> userInfoVOList = userList.stream()
                .map(user -> UserConverter.toUserInfoVO(user, roleMap.get(user.getRoleId())))
                .toList();

        PageVO<UserInfoVO> pageVO = new PageVO<>();
        pageVO.setTotal(page.getTotal());
        pageVO.setList(userInfoVOList);
        return pageVO;
    }

    /**
     * 更新用户状态：管理员启用或禁用某个用户。
     */
    public void updateUserStatus(UpdateUserStatusDTO updateUserStatusDTO) {
        if (updateUserStatusDTO == null) {
            throw new BusinessException(ResultCode.PARAM_NULL);
        }

        Long userId = updateUserStatusDTO.getUserId();
        Integer status = updateUserStatusDTO.getStatus();
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ResultCode.USER_STATUS_INVALID);
        }
        requireAdmin();

        User dbUser = userMapper.selectById(userId);
        if (dbUser == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }

        int rows = userMapper.updateUserStatus(userId, status);
        if (rows <= 0) {
            throw new BusinessException(ResultCode.USER_STATUS_UPDATE_FAILED);
        }
    }

    /**
     * 更新用户角色：管理员调整某个用户的角色。
     */
    public void updateUserRole(UpdateUserRoleDTO dto) {
        if (dto == null || dto.getUserId() == null || dto.getRoleId() == null) {
            throw new BusinessException(ResultCode.PARAM_NULL);
        }
        requireAdmin();
        User dbUser = userMapper.selectById(dto.getUserId());
        if (dbUser == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        Role role = roleMapper.selectById(dto.getRoleId());
        if (role == null) {
            throw new BusinessException(ResultCode.ROLE_NULL);
        }
        int rows = userMapper.updateUserRole(dto.getUserId(), dto.getRoleId());
        if (rows <= 0) {
            throw new BusinessException(ResultCode.USER_ROLE_UPDATE_FAILED);
        }
        logout(dto.getUserId());
    }

    /**
     * 业务方法 listRolesWithPermissions：封装 UserService 中对应的核心处理流程。
     */
    public List<RolePermissionVO> listRolesWithPermissions() {
        return roleMapper.selectAll().stream()
                .map(role -> {
                    RolePermissionVO vo = new RolePermissionVO();
                    vo.setRoleId(role.getId());
                    vo.setRoleCode(role.getRoleCode());
                    vo.setRoleName(role.getRoleName());
                    vo.setDescription(role.getDescription());
                    vo.setPermissionCodes(getPermissionCodes(role.getId(), role.getRoleCode()));
                    return vo;
                })
                .toList();
    }

    /**
     * 获取 activeUserSummary：返回当前对象里保存的这个值。
     */
    public ActiveUserSummaryVO getActiveUserSummary() {
        ActiveUserSummaryVO vo = new ActiveUserSummaryVO();
        if (stringRedisTemplate == null) {
            vo.setTodayActiveUsers(0L);
            vo.setWeekActiveUsers(0L);
            vo.setOnlineUsers(0L);
            return vo;
        }
        LocalDate today = LocalDate.now();
        int week = today.get(WeekFields.of(Locale.CHINA).weekOfWeekBasedYear());
        String dayKey = RedisKeyConstants.USER_ACTIVE_DAY_KEY + today.format(DAY_FORMATTER);
        String weekKey = RedisKeyConstants.USER_ACTIVE_WEEK_KEY + today.getYear() + String.format("%02d", week);
        vo.setTodayActiveUsers(sizeOfSet(dayKey));
        vo.setWeekActiveUsers(sizeOfSet(weekKey));
        vo.setOnlineUsers(countOnlineUsers());
        return vo;
    }

    /**
     * 业务方法 listActiveUsers：封装 UserService 中对应的核心处理流程。
     */
    public List<ActiveUserVO> listActiveUsers(Integer limit) {
        if (stringRedisTemplate == null) {
            return List.of();
        }
        int safeLimit = limit == null || limit < 1 ? 10 : Math.min(limit, 50);
        Set<String> userIds = stringRedisTemplate.opsForZSet()
                .reverseRange(RedisKeyConstants.USER_ACTIVE_RANK_KEY, 0, safeLimit - 1);
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        List<Long> ids = userIds.stream().map(Long::valueOf).toList();
        List<User> users = userMapper.selectByIds(ids);
        List<ActiveUserVO> result = new ArrayList<>();
        for (User user : users) {
            ActiveUserVO vo = new ActiveUserVO();
            vo.setUserId(user.getId());
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatar());
            Double score = stringRedisTemplate.opsForZSet()
                    .score(RedisKeyConstants.USER_ACTIVE_RANK_KEY, user.getId().toString());
            vo.setActivityScore(score == null ? 0D : score);
            String lastActive = stringRedisTemplate.opsForValue().get(RedisKeyConstants.USER_LAST_ACTIVE_KEY + user.getId());
            if (lastActive != null) {
                vo.setLastActiveTime(LocalDateTime.parse(lastActive, TIME_FORMATTER));
            }
            result.add(vo);
        }
        result.sort((a, b) -> Double.compare(
                b.getActivityScore() == null ? 0D : b.getActivityScore(),
                a.getActivityScore() == null ? 0D : a.getActivityScore()
        ));
        return result;
    }

    /**
     * 批量查询用户简要信息：给文章、评论等服务展示用户名和头像。
     */
    public List<UserSimpleVO> getBatchUserSimple(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        List<User> users = userMapper.selectByIds(userIds);
        return users.stream().map(user -> {
            UserSimpleVO vo = new UserSimpleVO();
            vo.setId(user.getId());
            vo.setName(user.getNickname());
            vo.setAvatar(user.getAvatar());
            return vo;
        }).toList();
    }

    /**
     * 获取 permissionCodes：返回当前对象里保存的这个值。
     */
    private List<String> getPermissionCodes(Long roleId, String roleCode) {
        List<String> permissionCodes = rolePermissionCacheService.getPermissionCodesByRoleId(roleId);
        if (!permissionCodes.isEmpty()) {
            return permissionCodes;
        }
        return rolePermissionCacheService.getPermissionCodesByRoleCode(roleCode);
    }

    /**
     * 校验当前用户是否管理员，不是管理员就抛出无权限异常。
     */
    private void requireAdmin() {
        if (!"ADMIN".equals(RequestUserContext.getRole())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }

    /**
     * 业务方法 sizeOfSet：封装 UserService 中对应的核心处理流程。
     */
    private Long sizeOfSet(String key) {
        Long size = stringRedisTemplate.opsForSet().size(key);
        return size == null ? 0L : size;
    }

    /**
     * 业务方法 countOnlineUsers：封装 UserService 中对应的核心处理流程。
     */
    private Long countOnlineUsers() {
        AtomicLong count = new AtomicLong(0);
        stringRedisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
            try (var cursor = connection.scan(
                    org.springframework.data.redis.core.ScanOptions.scanOptions()
                            .match(RedisKeyConstants.USER_ONLINE_KEY + "*")
                            .count(200)
                            .build())) {
                while (cursor.hasNext()) {
                    count.incrementAndGet();
                    cursor.next();
                }
            }
            return null;
        });
        return count.get();
    }

    /**
     * 获取 sessionInfo：返回当前对象里保存的这个值。
     */
    private SessionInfoVO getSessionInfo(Long userId) {
        if (stringRedisTemplate == null || userId == null) {
            return null;
        }
        String cached = stringRedisTemplate.opsForValue().get(RedisKeyConstants.USER_SESSION_INFO_KEY + userId);
        if (cached == null || cached.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(cached, SessionInfoVO.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * 业务方法 buildSessionInfo：封装 UserService 中对应的核心处理流程。
     */
    private SessionInfoVO buildSessionInfo(HttpServletRequest request) {
        String userAgent = safeHeader(request, "User-Agent");
        String ip = resolveClientIp(request);
        SessionInfoVO sessionInfo = new SessionInfoVO();
        sessionInfo.setLoginIp(ip);
        sessionInfo.setLocation(resolveLocation(ip));
        sessionInfo.setDevice(resolveDevice(userAgent));
        sessionInfo.setBrowser(resolveBrowser(userAgent));
        sessionInfo.setUserAgent(userAgent);
        sessionInfo.setLoginTime(LocalDateTime.now().format(TIME_FORMATTER));
        return sessionInfo;
    }

    /**
     * 业务方法 safeHeader：封装 UserService 中对应的核心处理流程。
     */
    private String safeHeader(HttpServletRequest request, String headerName) {
        String value = request.getHeader(headerName);
        return value == null || value.isBlank() ? "未知" : value;
    }

    /**
     * 业务方法 resolveClientIp：封装 UserService 中对应的核心处理流程。
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr == null || remoteAddr.isBlank() ? "未知" : remoteAddr;
    }

    /**
     * 业务方法 resolveLocation：封装 UserService 中对应的核心处理流程。
     */
    private String resolveLocation(String ip) {
        if (ip == null || ip.isBlank() || "未知".equals(ip)) {
            return "未知位置";
        }
        if ("127.0.0.1".equals(ip) || "::1".equals(ip) || ip.startsWith("192.168.") || ip.startsWith("10.")
                || ip.startsWith("172.16.") || ip.startsWith("172.17.") || ip.startsWith("172.18.")
                || ip.startsWith("172.19.") || ip.startsWith("172.20.") || ip.startsWith("172.21.")
                || ip.startsWith("172.22.") || ip.startsWith("172.23.") || ip.startsWith("172.24.")
                || ip.startsWith("172.25.") || ip.startsWith("172.26.") || ip.startsWith("172.27.")
                || ip.startsWith("172.28.") || ip.startsWith("172.29.") || ip.startsWith("172.30.")
                || ip.startsWith("172.31.")) {
            return "本机或局域网";
        }
        return "公网 IP";
    }

    /**
     * 业务方法 resolveDevice：封装 UserService 中对应的核心处理流程。
     */
    private String resolveDevice(String userAgent) {
        String source = userAgent == null ? "" : userAgent.toLowerCase(Locale.ROOT);
        if (source.contains("iphone")) {
            return "iPhone";
        }
        if (source.contains("ipad")) {
            return "iPad";
        }
        if (source.contains("android")) {
            return "Android 设备";
        }
        if (source.contains("windows")) {
            return "Windows 设备";
        }
        if (source.contains("macintosh") || source.contains("mac os x")) {
            return "Mac 设备";
        }
        if (source.contains("linux")) {
            return "Linux 设备";
        }
        return "未知设备";
    }

    /**
     * 业务方法 resolveBrowser：封装 UserService 中对应的核心处理流程。
     */
    private String resolveBrowser(String userAgent) {
        String source = userAgent == null ? "" : userAgent.toLowerCase(Locale.ROOT);
        if (source.contains("edg/")) {
            return "Microsoft Edge";
        }
        if (source.contains("chrome/") && !source.contains("edg/")) {
            return "Google Chrome";
        }
        if (source.contains("firefox/")) {
            return "Mozilla Firefox";
        }
        if (source.contains("safari/") && !source.contains("chrome/")) {
            return "Safari";
        }
        if (source.contains("micromessenger")) {
            return "微信内置浏览器";
        }
        return "未知浏览器";
    }

    /**
     * 业务方法 validateUniqueProfileFields：封装 UserService 中对应的核心处理流程。
     */
    private void validateUniqueProfileFields(String nickname, String email, String phone, Long excludeUserId) {
        if (nickname != null && !nickname.isBlank()) {
            User nicknameOwner = excludeUserId == null
                    ? userMapper.selectByNickname(nickname)
                    : userMapper.selectByNicknameExcludeId(nickname, excludeUserId);
            if (nicknameOwner != null) {
                throw new BusinessException(ResultCode.USER_UPDATE_FAILED.getCode(), "昵称已存在，请重新填写");
            }
        }
        if (email != null && !email.isBlank()) {
            User emailOwner = excludeUserId == null
                    ? userMapper.selectByEmail(email)
                    : userMapper.selectByEmailExcludeId(email, excludeUserId);
            if (emailOwner != null) {
                throw new BusinessException(ResultCode.USER_UPDATE_FAILED.getCode(), "邮箱已存在，请重新填写");
            }
        }
        if (phone != null && !phone.isBlank()) {
            User phoneOwner = excludeUserId == null
                    ? userMapper.selectByPhone(phone)
                    : userMapper.selectByPhoneExcludeId(phone, excludeUserId);
            if (phoneOwner != null) {
                throw new BusinessException(ResultCode.USER_UPDATE_FAILED.getCode(), "手机号已存在，请重新填写");
            }
        }
    }
}
