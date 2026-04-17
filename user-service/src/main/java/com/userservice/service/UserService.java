package com.userservice.service;

import com.blogcommon.constant.RedisKeyConstants;
import com.blogcommon.enums.ResultCode;
import com.blogcommon.exception.BusinessException;
import com.blogcommon.logging.DbWriteAuditLogger;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class UserService {
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final long MAX_AVATAR_SIZE = 5L * 1024 * 1024;

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
    @Value("${app.avatar.upload-dir}")
    private String avatarUploadDir;

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

    public LoginVO login(LoginDTO loginDTO) {
        User dbUser = userMapper.selectByUsername(loginDTO.getUsername());
        if (dbUser == null) {
            throw new BusinessException(ResultCode.USERNAME_NOT_EXIST);
        }
        Role role = roleMapper.selectById(dbUser.getRoleId());
        if (role == null) {
            throw new BusinessException(ResultCode.ROLE_NULL);
        }
        if (dbUser.getStatus() != null && dbUser.getStatus() == 0) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }
        if (!passwordEncoder.matches(loginDTO.getPassword(), dbUser.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }

        String token = JwtUtil.createToken(dbUser.getId(), dbUser.getUsername(), role.getRoleCode());
        cacheToken(dbUser.getId(), token);
        userActivityService.recordActivity(dbUser.getId());

        LoginUserVO loginUserVO = UserConverter.toLoginUserVO(dbUser);
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setUser(loginUserVO);
        return loginVO;
    }

    private void cacheToken(Long userId, String token) {
        if (stringRedisTemplate != null) {
            String key = RedisKeyConstants.USER_TOKEN_KEY + userId;
            stringRedisTemplate.opsForValue().set(key, token, RedisKeyConstants.USER_TOKEN_EXPIRE, TimeUnit.SECONDS);
        }
    }

    public boolean validateToken(Long userId, String token) {
        if (stringRedisTemplate == null) {
            return true;
        }
        String key = RedisKeyConstants.USER_TOKEN_KEY + userId;
        String cachedToken = stringRedisTemplate.opsForValue().get(key);
        return token != null && token.equals(cachedToken);
    }

    public void logout(Long userId) {
        if (stringRedisTemplate != null && userId != null) {
            String key = RedisKeyConstants.USER_TOKEN_KEY + userId;
            stringRedisTemplate.delete(key);
            stringRedisTemplate.delete(RedisKeyConstants.USER_ONLINE_KEY + userId);
            stringRedisTemplate.delete(RedisKeyConstants.USER_SESSION_INFO_KEY + userId);
        }
    }

    public void kickout(Long userId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.PARAM_NULL);
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        logout(userId);
    }

    public void refreshToken(Long userId) {
        if (stringRedisTemplate != null && userId != null) {
            String key = RedisKeyConstants.USER_TOKEN_KEY + userId;
            stringRedisTemplate.expire(key, RedisKeyConstants.USER_TOKEN_EXPIRE, TimeUnit.SECONDS);
            stringRedisTemplate.expire(RedisKeyConstants.USER_SESSION_INFO_KEY + userId, RedisKeyConstants.USER_TOKEN_EXPIRE, TimeUnit.SECONDS);
        }
        userActivityService.recordActivity(userId);
    }

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

    public String uploadAvatar(Long userId, MultipartFile file) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "请选择头像文件");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "头像大小不能超过5MB");
        }

        String extension = resolveAvatarExtension(file.getOriginalFilename(), file.getContentType());
        String filename = userId + "-" + UUID.randomUUID().toString().replace("-", "") + extension;

        try {
            Path uploadDir = Path.of(avatarUploadDir);
            Files.createDirectories(uploadDir);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, uploadDir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new BusinessException(ResultCode.USER_UPDATE_FAILED.getCode(), "头像上传失败");
        }

        return "/api/user/avatar/" + filename;
    }

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
        List<UserInfoVO> userInfoVOList = userList.stream()
                .map(user -> {
                    Role role = roleMapper.selectById(user.getRoleId());
                    return UserConverter.toUserInfoVO(user, role);
                })
                .toList();

        PageVO<UserInfoVO> pageVO = new PageVO<>();
        pageVO.setTotal(page.getTotal());
        pageVO.setList(userInfoVOList);
        return pageVO;
    }

    public void updateUserStatus(UpdateUserStatusDTO updateUserStatusDTO) {
        if (updateUserStatusDTO == null) {
            throw new BusinessException(ResultCode.PARAM_NULL);
        }

        Long userId = updateUserStatusDTO.getUserId();
        Integer status = updateUserStatusDTO.getStatus();
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ResultCode.USER_STATUS_INVALID);
        }

        User dbUser = userMapper.selectById(userId);
        if (dbUser == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }

        int rows = userMapper.updateUserStatus(userId, status);
        if (rows <= 0) {
            throw new BusinessException(ResultCode.USER_STATUS_UPDATE_FAILED);
        }
    }

    public void updateUserRole(UpdateUserRoleDTO dto) {
        if (dto == null || dto.getUserId() == null || dto.getRoleId() == null) {
            throw new BusinessException(ResultCode.PARAM_NULL);
        }
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

    private List<String> getPermissionCodes(Long roleId, String roleCode) {
        List<String> permissionCodes = rolePermissionCacheService.getPermissionCodesByRoleId(roleId);
        if (!permissionCodes.isEmpty()) {
            return permissionCodes;
        }
        return rolePermissionCacheService.getPermissionCodesByRoleCode(roleCode);
    }

    private Long sizeOfSet(String key) {
        Long size = stringRedisTemplate.opsForSet().size(key);
        return size == null ? 0L : size;
    }

    private Long countOnlineUsers() {
        Collection<String> keys = stringRedisTemplate.keys(RedisKeyConstants.USER_ONLINE_KEY + "*");
        return keys == null ? 0L : (long) keys.size();
    }

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

    private String safeHeader(HttpServletRequest request, String headerName) {
        String value = request.getHeader(headerName);
        return value == null || value.isBlank() ? "未知" : value;
    }

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

    private String resolveAvatarExtension(String originalFilename, String contentType) {
        String extension = "";
        if (originalFilename != null) {
            int lastDotIndex = originalFilename.lastIndexOf('.');
            if (lastDotIndex >= 0) {
                extension = originalFilename.substring(lastDotIndex).toLowerCase(Locale.ROOT);
            }
        }

        return switch (extension) {
            case ".jpg", ".jpeg", ".png", ".webp", ".gif" -> extension;
            default -> mapContentTypeToExtension(contentType);
        };
    }

    private String mapContentTypeToExtension(String contentType) {
        if (contentType == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "仅支持 jpg、jpeg、png、webp、gif 格式头像");
        }
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "仅支持 jpg、jpeg、png、webp、gif 格式头像");
        };
    }

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
