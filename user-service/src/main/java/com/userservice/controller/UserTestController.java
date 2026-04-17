package com.userservice.controller;

import com.blogcommon.auth.AuthConstants;
import com.blogcommon.result.Result;
import com.blogcommon.util.JwtUtil;
import com.userservice.config.AdminOnly;
import com.userservice.config.RequirePermission;
import com.userservice.config.UserContext;
import com.userservice.dto.LoginDTO;
import com.userservice.dto.RegisterDTO;
import com.userservice.dto.ResetPasswordByPhoneDTO;
import com.userservice.dto.UpdatePasswordDTO;
import com.userservice.dto.UpdateUserInfoDTO;
import com.userservice.dto.UpdateUserRoleDTO;
import com.userservice.dto.UpdateUserStatusDTO;
import com.userservice.dto.UserPageQueryDTO;
import com.userservice.service.UserService;
import com.userservice.vo.ActiveUserSummaryVO;
import com.userservice.vo.ActiveUserVO;
import com.userservice.vo.CurrentUserVO;
import com.userservice.vo.LoginVO;
import com.userservice.vo.PageVO;
import com.userservice.vo.RolePermissionVO;
import com.userservice.vo.UserInfoVO;
import com.userservice.vo.UserSimpleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/user")
@Tag(name = "用户模块", description = "用户登录、资料、权限与活跃度接口")
public class UserTestController {
    @Value("${app.avatar.upload-dir}")
    private String avatarUploadDir;

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public Result<String> register(@Valid @RequestBody RegisterDTO registerDTO) {
        userService.register(registerDTO);
        return Result.success("注册成功");
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO, HttpServletResponse response, HttpServletRequest request) {
        LoginVO loginVO = userService.login(loginDTO);
        userService.recordSessionInfo(loginVO.getUser().getUsername(), request);
        Cookie cookie = new Cookie(AuthConstants.AUTH_COOKIE_NAME, loginVO.getToken());
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(AuthConstants.AUTH_COOKIE_MAX_AGE);
        response.addCookie(cookie);
        loginVO.setToken(null);
        return new Result<>(200, "登录成功", loginVO);
    }

    @GetMapping("/parse")
    @Operation(summary = "解析token")
    public Result<Object> parseToken(@RequestParam("token") String token) {
        return Result.success(JwtUtil.parseToken(token));
    }

    @GetMapping("/check")
    @Operation(summary = "校验注册字段是否可用")
    public Result<Boolean> checkAvailability(@RequestParam("field") String field, @RequestParam("value") String value) {
        return Result.success(userService.isFieldAvailable(field, value));
    }

    @GetMapping("/me")
    @Operation(summary = "查询当前用户信息")
    public Result<CurrentUserVO> me() {
        return Result.success(userService.getCurrentUserInfo(UserContext.getUserId()));
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录")
    public Result<String> logout(HttpServletResponse response) {
        userService.logout(UserContext.getUserId());
        Cookie cookie = new Cookie(AuthConstants.AUTH_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return Result.success("退出成功");
    }

    @PutMapping("/info")
    @Operation(summary = "更新用户信息")
    public Result<String> updateUserInfo(@Valid @RequestBody UpdateUserInfoDTO updateUserInfoDTO) {
        userService.updateCurrentUserInfo(UserContext.getUserId(), updateUserInfoDTO);
        return Result.success("修改成功");
    }

    @PostMapping("/avatar/upload")
    @Operation(summary = "上传头像")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return Result.success(userService.uploadAvatar(UserContext.getUserId(), file));
    }

    @GetMapping("/avatar/{filename:.+}")
    @Operation(summary = "获取头像文件")
    public ResponseEntity<Resource> getAvatar(@PathVariable("filename") String filename) throws MalformedURLException {
        Path uploadDir = Path.of(avatarUploadDir).toAbsolutePath().normalize();
        Path avatarPath = uploadDir.resolve(filename).normalize();
        if (!avatarPath.startsWith(uploadDir)) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new UrlResource(avatarPath.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        String lowerName = filename.toLowerCase();
        if (lowerName.endsWith(".png")) {
            mediaType = MediaType.IMAGE_PNG;
        } else if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
            mediaType = MediaType.IMAGE_JPEG;
        } else if (lowerName.endsWith(".gif")) {
            mediaType = MediaType.IMAGE_GIF;
        } else if (lowerName.endsWith(".webp")) {
            mediaType = MediaType.parseMediaType("image/webp");
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .contentType(mediaType)
                .body(resource);
    }

    @PutMapping("/password")
    @Operation(summary = "修改密码")
    public Result<String> updatePassword(@Valid @RequestBody UpdatePasswordDTO updatePasswordDTO) {
        userService.updatePassword(UserContext.getUserId(), updatePasswordDTO);
        return Result.success("密码修改成功");
    }

    @PostMapping("/password/reset")
    @Operation(summary = "通过用户名和手机号重置密码")
    public Result<String> resetPasswordByPhone(@Valid @RequestBody ResetPasswordByPhoneDTO dto) {
        userService.resetPasswordByPhone(dto);
        return Result.success("密码重置成功");
    }

    @GetMapping("/page")
    @RequirePermission("user:manage")
    @Operation(summary = "分页查询用户")
    public Result<PageVO<UserInfoVO>> pageUsers(UserPageQueryDTO queryDTO) {
        return Result.success(userService.pageUsers(queryDTO));
    }

    @PutMapping("/status")
    @AdminOnly
    @Operation(summary = "修改用户状态")
    public Result<String> updateUserStatus(@Valid @RequestBody UpdateUserStatusDTO updateUserStatusDTO) {
        userService.updateUserStatus(updateUserStatusDTO);
        return Result.success("用户状态修改成功");
    }

    @PutMapping("/role")
    @RequirePermission("role:assign")
    @Operation(summary = "分配用户角色")
    public Result<String> updateUserRole(@Valid @RequestBody UpdateUserRoleDTO dto) {
        userService.updateUserRole(dto);
        return Result.success("用户角色更新成功");
    }

    @GetMapping("/roles")
    @RequirePermission("role:view")
    @Operation(summary = "查询角色权限")
    public Result<List<RolePermissionVO>> listRoles() {
        return Result.success(userService.listRolesWithPermissions());
    }

    @GetMapping("/context")
    @Operation(summary = "打印用户上下文")
    public Result<String> context() {
        return Result.success("userId=" + UserContext.getUserId() + ", role=" + UserContext.getRole());
    }

    @PostMapping("/batch/simple")
    @Operation(summary = "批量获取用户简要信息")
    public Result<List<UserSimpleVO>> getBatchUserSimple(@RequestBody List<Long> userIds) {
        return Result.success(userService.getBatchUserSimple(userIds));
    }

    @GetMapping("/token/validate")
    @Operation(summary = "校验Token")
    public Result<Boolean> validateToken(@RequestParam("userId") Long userId, @RequestParam("token") String token) {
        return Result.success(userService.validateToken(userId, token));
    }

    @PostMapping("/kickout/{userId}")
    @AdminOnly
    @Operation(summary = "踢用户下线")
    public Result<String> kickout(@PathVariable("userId") Long userId) {
        userService.kickout(userId);
        return Result.success("已将该用户踢下线");
    }

    @PostMapping("/token/refresh")
    @Operation(summary = "刷新Token")
    public Result<String> refreshToken() {
        userService.refreshToken(UserContext.getUserId());
        return Result.success("Token刷新成功");
    }

    @GetMapping("/activity/summary")
    @RequirePermission("stats:view")
    @Operation(summary = "活跃用户摘要")
    public Result<ActiveUserSummaryVO> activitySummary() {
        return Result.success(userService.getActiveUserSummary());
    }

    @GetMapping("/activity/ranking")
    @RequirePermission("stats:view")
    @Operation(summary = "活跃用户排行")
    public Result<List<ActiveUserVO>> activityRanking(@RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        return Result.success(userService.listActiveUsers(limit));
    }
}
