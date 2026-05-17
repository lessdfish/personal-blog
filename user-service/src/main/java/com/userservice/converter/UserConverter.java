package com.userservice.converter;

import com.userservice.entity.Role;
import com.userservice.entity.User;
import com.userservice.vo.CurrentUserVO;
import com.userservice.vo.LoginUserVO;
import com.userservice.vo.UserInfoVO;

import java.util.List;

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

    /**
     * 转换 toLoginUserVO 数据：把一种对象结构整理成另一种返回结构。
     */
    public static LoginUserVO toLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO vo = new LoginUserVO();
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        return vo;
    }

    /**
     * 转换 toCurrentUserVO 数据：把一种对象结构整理成另一种返回结构。
     */
    public static CurrentUserVO toCurrentUserVO(User user) {
        if (user == null) {
            return null;
        }
        CurrentUserVO vo = new CurrentUserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        return vo;
    }

    /**
     * 转换 toUserInfoVO 数据：把一种对象结构整理成另一种返回结构。
     */
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

    /**
     * 转换 toUserInfoVO 数据：把一种对象结构整理成另一种返回结构。
     */
    public static UserInfoVO toUserInfoVO(User user,Role role){
        UserInfoVO vo = toUserInfoVO(user);
        if (vo != null && role != null) {
            vo.setRoleCode(role.getRoleCode());
            vo.setRoleName(role.getRoleName());
        }
        return vo;
    }

    /**
     * 转换 toUserInfoVO 数据：把一种对象结构整理成另一种返回结构。
     */
    public static UserInfoVO toUserInfoVO(User user, Role role, List<String> permissionCodes){
        UserInfoVO vo = toUserInfoVO(user, role);
        if (vo != null) {
            vo.setPermissionCodes(permissionCodes);
        }
        return vo;
    }
}
