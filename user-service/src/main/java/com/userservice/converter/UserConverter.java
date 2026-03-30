package com.userservice.converter;

import com.userservice.entity.Role;
import com.userservice.entity.User;
import com.userservice.vo.UserInfoVO;

/**
 * ClassName:UserConverter
 * Package:com.userservice.converter
 * Description:
 *
 * @Author:lyp
 * @Create:2026/3/27 - 23:18
 * @Version: v1.0
 *
 */

public class UserConverter {

    public static UserInfoVO toUserInfoVO(User user) {
        if (user == null) {
            return null;
        }

        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setStatus(user.getStatus());

        return vo;
    }

    public static UserInfoVO toUserInfoVO(User user,Role role){
        UserInfoVO vo = toUserInfoVO(user);
        if (vo != null && role != null) {
            vo.setRoleCode(role.getRoleCode());
            vo.setRoleName(role.getRoleName());
        }
        return vo;
    }
}
