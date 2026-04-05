package com.userservice.service;

import com.blogcommon.constant.RedisKeyConstants;
import com.blogcommon.enums.ResultCode;
import com.blogcommon.exception.BusinessException;
import com.blogcommon.util.JwtUtil;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.userservice.converter.UserConverter;
import com.userservice.dto.LoginDTO;
import com.userservice.dto.RegisterDTO;
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
import com.userservice.vo.LoginVO;
import com.userservice.vo.PageVO;
import com.userservice.vo.RolePermissionVO;
import com.userservice.vo.UserInfoVO;
import com.userservice.vo.UserSimpleVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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
    private UserActivityService userActivityService;
    @Autowired
    private RolePermissionCacheService rolePermissionCacheService;

    public void register(RegisterDTO registerDTO) {
        User dbUser = userMapper.selectByUsername(registerDTO.getUsername());
        if (dbUser != null) {
            throw new BusinessException(ResultCode.USERNAME_EXIST);
        }

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

        UserInfoVO userInfoVO = UserConverter.toUserInfoVO(dbUser, role, getPermissionCodes(role.getId(), role.getRoleCode()));
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setUserInfoVO(userInfoVO);
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
        }
        userActivityService.recordActivity(userId);
    }

    public UserInfoVO getCurrentUserInfo(Long userId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USERNAME_NOT_EXIST);
        }
        Role role = roleMapper.selectById(user.getRoleId());
        return UserConverter.toUserInfoVO(user, role, role == null ? List.of() : getPermissionCodes(role.getId(), role.getRoleCode()));
    }

    public void updateCurrentUserInfo(Long userId, UpdateUserInfoDTO updateUserInfoDTO) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        User dbUser = userMapper.selectById(userId);
        if (dbUser == null) {
            throw new BusinessException(ResultCode.USERNAME_NOT_EXIST);
        }

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
}
