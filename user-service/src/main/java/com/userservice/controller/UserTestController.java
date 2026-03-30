package com.userservice.controller;

import com.blogcommon.result.Result;
import com.blogcommon.util.JwtUtil;
import com.userservice.config.AdminOnly;
import com.userservice.config.UserContext;
import com.userservice.dto.*;
import com.userservice.entity.User;
import com.userservice.mapper.UserMapper;
import com.userservice.service.UserService;
import com.userservice.vo.LoginVO;
import com.userservice.vo.PageVO;
import com.userservice.vo.UserInfoVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * ClassName:UserTestController
 * Package:com.userservice.user
 * Description:
 *
 * @Author:lyp
 * @Create:2026/3/26 - 22:08
 * @Version: v1.0
 *
 */

@RestController
@RequestMapping("/user")
public class UserTestController {

    @Autowired
    private UserService userService;
    @Autowired
    private UserMapper userMapper;

    @PostMapping("/register")
    public Result<String> register(@Valid @RequestBody RegisterDTO registerDTO) {
        userService.register(registerDTO);
        return Result.success("注册成功");
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        LoginVO loginVO = userService.login(loginDTO);
        return Result.success(loginVO);
    }

    @GetMapping("/parse")
    public Result<Object> parseToken(@RequestParam("token") String token) {
        return Result.success(JwtUtil.parseToken(token));
    }

    @GetMapping("/me")
    public Result<UserInfoVO> me(){
        Long userId = UserContext.getUserId();
        UserInfoVO userInfoVO = userService.getCurrentUserInfo(userId);
        return Result.success(userInfoVO);
    }

    @PostMapping("/logout")
    public Result<String> logout(){
        return Result.success("退出成功");
    }

    @PutMapping("/info")
    public Result<String> updateUserInfo(@Valid @RequestBody UpdateUserInfoDTO updateUserInfoDTO){
        Long userId = UserContext.getUserId();
        userService.updateCurrentUserInfo(userId,updateUserInfoDTO);
        return Result.success("修改成功");
    }

    @PutMapping("/password")
    public Result<String> updatePassword(@Valid @RequestBody UpdatePasswordDTO updatePasswordDTO) {
        Long userId = UserContext.getUserId();
        userService.updatePassword(userId, updatePasswordDTO);
        return Result.success("密码修改成功");
    }

    @GetMapping("/page")
    public Result<PageVO<UserInfoVO>> pageUsers(UserPageQueryDTO queryDTO){
        PageVO<UserInfoVO> pageVO = userService.pageUsers(queryDTO);
        return Result.success(pageVO);
    }

    @PutMapping("/status")
    @AdminOnly
    public Result<String> updateUserStatus(@Valid @RequestBody UpdateUserStatusDTO updateUserStatusDTO) {
        userService.updateUserStatus(updateUserStatusDTO);
        return Result.success("用户状态修改成功");
    }
    @GetMapping("/context")
    public Result<String> context() {
        Long userId = UserContext.getUserId();
        String role = UserContext.getRole();
        return Result.success("userId=" + userId + ", role=" + role);
    }
}
