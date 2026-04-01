package com.userservice.service;

import com.alibaba.nacos.common.utils.StringUtils;
import com.blogcommon.constant.RedisKeyConstants;
import com.blogcommon.enums.ResultCode;
import com.blogcommon.exception.BusinessException;
import com.blogcommon.result.Result;
import com.blogcommon.util.JwtUtil;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.userservice.converter.UserConverter;
import com.userservice.dto.*;
import com.userservice.entity.Role;
import com.userservice.entity.User;
import com.userservice.mapper.RoleMapper;
import com.userservice.mapper.UserMapper;
import com.userservice.vo.LoginVO;
import com.userservice.vo.PageVO;
import com.userservice.vo.UserInfoVO;
import com.userservice.vo.UserSimpleVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * ClassName:UserService
 * Package:com.userservice.service
 * Description:
 *
 * @Author:lyp
 * @Create:2026/3/27 - 00:25
 * @Version: v1.0
 *
 */
@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RoleMapper roleMapper;
    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    //注册
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

        userMapper.insert(user);
    }

    //登录
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

        String token = JwtUtil.createToken(dbUser.getId(), dbUser.getUsername(),role.getRoleCode());

        // 缓存token到Redis
        cacheToken(dbUser.getId(), token);

        UserInfoVO userInfoVO = UserConverter.toUserInfoVO(dbUser,role);

        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setUserInfoVO(userInfoVO);
        return loginVO;
    }

    /**
     * 缓存Token到Redis
     */
    private void cacheToken(Long userId, String token) {
        if (stringRedisTemplate != null) {
            String key = RedisKeyConstants.USER_TOKEN_KEY + userId;
            stringRedisTemplate.opsForValue().set(key, token, 
                    RedisKeyConstants.USER_TOKEN_EXPIRE, TimeUnit.SECONDS);
        }
    }

    /**
     * 验证Token是否有效（是否在Redis中）
     */
    public boolean validateToken(Long userId, String token) {
        if (stringRedisTemplate == null) {
            return true; // Redis未启用，默认通过
        }
        String key = RedisKeyConstants.USER_TOKEN_KEY + userId;
        String cachedToken = stringRedisTemplate.opsForValue().get(key);
        return token != null && token.equals(cachedToken);
    }

    /**
     * 用户登出（删除Redis中的Token）
     */
    public void logout(Long userId) {
        if (stringRedisTemplate != null && userId != null) {
            String key = RedisKeyConstants.USER_TOKEN_KEY + userId;
            stringRedisTemplate.delete(key);
        }
    }

    /**
     * 踢用户下线（管理员功能）
     */
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

    /**
     * 刷新Token过期时间
     */
    public void refreshToken(Long userId) {
        if (stringRedisTemplate != null && userId != null) {
            String key = RedisKeyConstants.USER_TOKEN_KEY + userId;
            stringRedisTemplate.expire(key, RedisKeyConstants.USER_TOKEN_EXPIRE, TimeUnit.SECONDS);
        }
    }

    //查询当前用户信息
    public UserInfoVO getCurrentUserInfo(Long userId){
        if(userId == null){
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USERNAME_NOT_EXIST);
        }
        UserInfoVO userInfoVO = UserConverter.toUserInfoVO(user);

        return userInfoVO;

    }
    //更新用户信息
    public void updateCurrentUserInfo(Long userId, UpdateUserInfoDTO updateUserInfoDTO){
        if(userId == null){
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
        if(rows<=0){
            throw new BusinessException(ResultCode.USER_UPDATE_FAILED);
        }
    }
    //更新密码
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

    //分页
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
        List<User> userList = userMapper.selectUserListByCondition(queryDTO.getUsername(),queryDTO.getStatus());

        List<UserInfoVO> userInfoVOList = userList.stream()
                .map(UserConverter::toUserInfoVO)
                .toList();

        PageVO<UserInfoVO> pageVO = new PageVO<>();
        pageVO.setTotal(page.getTotal());
        pageVO.setList(userInfoVOList);

        return pageVO;
    }

    //更新用户状态信息
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
}
