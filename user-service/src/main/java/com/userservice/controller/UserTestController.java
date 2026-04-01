package com.userservice.controller;

import com.blogcommon.result.Result;
import com.blogcommon.util.JwtUtil;
import com.userservice.config.AdminOnly;
import com.userservice.config.UserContext;
import com.userservice.dto.*;
import com.userservice.service.UserService;
import com.userservice.vo.LoginVO;
import com.userservice.vo.PageVO;
import com.userservice.vo.UserInfoVO;
import com.userservice.vo.UserSimpleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
@Tag(name = "用户模块",description = "用户登录接口")
public class UserTestController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    @Operation(summary = "用户注册",description = "新用户注册接口")
    public Result<String> register(@Valid @RequestBody RegisterDTO registerDTO) {
        userService.register(registerDTO);
        return Result.success("注册成功");
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录",description = "用户登录接口")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        LoginVO loginVO = userService.login(loginDTO);
        return Result.success(loginVO);
    }

    @GetMapping("/parse")
    @Operation(summary = "解析token",description = "解析token")
    public Result<Object> parseToken(@RequestParam("token") String token) {
        return Result.success(JwtUtil.parseToken(token));
    }

    @GetMapping("/me")
    @Operation(summary = "查询用户信息",description = "查询用户信息接口")
    public Result<UserInfoVO> me(){
        Long userId = UserContext.getUserId();
        UserInfoVO userInfoVO = userService.getCurrentUserInfo(userId);
        return Result.success(userInfoVO);
    }

    @PostMapping("/logout")
    @Operation(summary = "用户登出",description = "用户退出登录接口")
    public Result<String> logout(){
        Long userId = UserContext.getUserId();
        userService.logout(userId);
        return Result.success("退出成功");
    }

    @PutMapping("/info")
    @Operation(summary = "更新用户信息",description = "更新用户信息接口")
    public Result<String> updateUserInfo(@Valid @RequestBody UpdateUserInfoDTO updateUserInfoDTO){
        Long userId = UserContext.getUserId();
        userService.updateCurrentUserInfo(userId,updateUserInfoDTO);
        return Result.success("修改成功");
    }

    @PutMapping("/password")
    @Operation(summary = "修改密码",description = "修改密码")
    public Result<String> updatePassword(@Valid @RequestBody UpdatePasswordDTO updatePasswordDTO) {
        Long userId = UserContext.getUserId();
        userService.updatePassword(userId, updatePasswordDTO);
        return Result.success("密码修改成功");
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询",description = "分页查询用户信息接口")
    public Result<PageVO<UserInfoVO>> pageUsers(UserPageQueryDTO queryDTO){
        PageVO<UserInfoVO> pageVO = userService.pageUsers(queryDTO);
        return Result.success(pageVO);
    }

    @PutMapping("/status")
    @AdminOnly
    @Operation(summary = "修改用户状态(仅限管理员)",description = "修改用户状态接口")
    public Result<String> updateUserStatus(@Valid @RequestBody UpdateUserStatusDTO updateUserStatusDTO) {
        userService.updateUserStatus(updateUserStatusDTO);
        return Result.success("用户状态修改成功");
    }
    @GetMapping("/context")
    @Operation(summary = "打印用户权限",description = "查询用户权限接口")
    public Result<String> context() {
        Long userId = UserContext.getUserId();
        String role = UserContext.getRole();
        return Result.success("userId=" + userId + ", role=" + role);
    }

    @PostMapping("/batch/simple")
    @Operation(summary = "批量获取用户简要信息", description = "供其他服务调用，返回用户ID、昵称、头像")
    public Result<List<UserSimpleVO>> getBatchUserSimple(@RequestBody List<Long> userIds) {
        return Result.success(userService.getBatchUserSimple(userIds));
    }

    @GetMapping("/token/validate")
    @Operation(summary = "验证Token是否有效", description = "检查Token是否在Redis中存在")
    public Result<Boolean> validateToken(@RequestParam("userId") Long userId, 
                                          @RequestParam("token") String token) {
        return Result.success(userService.validateToken(userId, token));
    }

    @PostMapping("/kickout/{userId}")
    @AdminOnly
    @Operation(summary = "踢用户下线", description = "管理员强制用户下线")
    public Result<String> kickout(@PathVariable("userId") Long userId) {
        userService.kickout(userId);
        return Result.success("已将该用户踢下线");
    }

    @PostMapping("/token/refresh")
    @Operation(summary = "刷新Token", description = "刷新Token过期时间")
    public Result<String> refreshToken() {
        Long userId = UserContext.getUserId();
        userService.refreshToken(userId);
        return Result.success("Token刷新成功");
    }
}
