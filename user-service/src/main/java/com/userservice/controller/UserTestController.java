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
import com.userservice.config.AvatarUploadProperties;
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
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseCookie;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/user")
@Tag(name = "用户模块", description = "用户登录、资料、权限与活跃度接口")
public class UserTestController {
    @Value("${auth.cookie.secure:true}")
    private boolean cookieSecure;
    @Value("${auth.cookie.same-site:Lax}")
    private String cookieSameSite;

    @Autowired
    private UserService userService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private AvatarUploadProperties avatarUploadProperties;

    /**
     * 注册用户：校验注册信息，加密密码，然后保存新用户。
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public Result<String> register(@Valid @RequestBody RegisterDTO registerDTO) {
        userService.register(registerDTO);
        return Result.success("注册成功");
    }

    /**
     * 用户登录：校验账号密码，生成 token，并返回登录用户信息。
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO, HttpServletResponse response, HttpServletRequest request) {
        LoginVO loginVO = userService.login(loginDTO);
        userService.recordSessionInfo(loginVO.getUser().getUsername(), request);
        addCookie(response, AuthConstants.AUTH_COOKIE_NAME, loginVO.getToken(), "/", AuthConstants.AUTH_COOKIE_MAX_AGE);
        addCookie(response, AuthConstants.REFRESH_COOKIE_NAME, loginVO.getRefreshToken(),
                AuthConstants.REFRESH_COOKIE_PATH, AuthConstants.REFRESH_COOKIE_MAX_AGE);
        addCsrfCookie(response);
        loginVO.setToken(null);
        loginVO.setRefreshToken(null);
        return new Result<>(200, "登录成功", loginVO);
    }

    /**
     * 处理 parseToken 接口：接收前端请求，调用业务层后返回统一结果。
     */
    @GetMapping("/parse")
    @Operation(summary = "解析token")
    public Result<Object> parseToken(@RequestParam("token") String token) {
        return Result.success(jwtUtil.parseToken(token));
    }

    /**
     * 处理 checkAvailability 接口：接收前端请求，调用业务层后返回统一结果。
     */
    @GetMapping("/check")
    @Operation(summary = "校验注册字段是否可用")
    public Result<Boolean> checkAvailability(@RequestParam("field") String field, @RequestParam("value") String value) {
        return Result.success(userService.isFieldAvailable(field, value));
    }

    /**
     * 处理 me 接口：接收前端请求，调用业务层后返回统一结果。
     */
    @GetMapping("/me")
    @Operation(summary = "查询当前用户信息")
    public Result<CurrentUserVO> me() {
        return Result.success(userService.getCurrentUserInfo(UserContext.getUserId()));
    }

    /**
     * 退出登录：清理当前用户的登录状态和 token。
     */
    @PostMapping("/logout")
    @Operation(summary = "退出登录")
    public Result<String> logout(HttpServletResponse response) {
        userService.logout(UserContext.getUserId());
        addCookie(response, AuthConstants.AUTH_COOKIE_NAME, "", "/", 0);
        addCookie(response, AuthConstants.REFRESH_COOKIE_NAME, "", AuthConstants.REFRESH_COOKIE_PATH, 0);
        addReadableCookie(response, AuthConstants.CSRF_COOKIE_NAME, "", "/", 0);
        return Result.success("退出成功");
    }

    /**
     * 更新用户资料：修改昵称、头像、邮箱或手机号等信息。
     */
    @PutMapping("/info")
    @Operation(summary = "更新用户信息")
    public Result<String> updateUserInfo(@Valid @RequestBody UpdateUserInfoDTO updateUserInfoDTO) {
        userService.updateCurrentUserInfo(UserContext.getUserId(), updateUserInfoDTO);
        return Result.success("修改成功");
    }

    /**
     * 处理 uploadAvatar 接口：接收前端请求，调用业务层后返回统一结果。
     */
    @PostMapping("/avatar/upload")
    @Operation(summary = "上传头像")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return Result.success(userService.uploadAvatar(UserContext.getUserId(), file));
    }

    /**
     * 获取 avatar：返回当前对象里保存的这个值。
     */
    @GetMapping("/avatar/{filename:.+}")
    @Operation(summary = "获取头像文件")
    public ResponseEntity<Resource> getAvatar(@PathVariable("filename") String filename) throws MalformedURLException {
        if (!isGeneratedAvatarName(filename)) {
            return ResponseEntity.notFound().build();
        }
        Path uploadDir = Path.of(avatarUploadProperties.getUploadDir()).toAbsolutePath().normalize();
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

    /**
     * 判断 generatedAvatarName：返回当前开关或状态是否成立。
     */
    private boolean isGeneratedAvatarName(String filename) {
        return filename != null && filename.matches("\\d+-[a-f0-9]{32}\\.(jpg|png|gif|webp)");
    }

    /**
     * 修改密码：校验旧密码后保存新密码。
     */
    @PutMapping("/password")
    @Operation(summary = "修改密码")
    public Result<String> updatePassword(@Valid @RequestBody UpdatePasswordDTO updatePasswordDTO) {
        userService.updatePassword(UserContext.getUserId(), updatePasswordDTO);
        return Result.success("密码修改成功");
    }

    /**
     * 通过手机号重置密码：校验用户和手机号后设置新密码。
     */
    @PostMapping("/password/reset")
    @Operation(summary = "通过用户名和手机号重置密码")
    public Result<String> resetPasswordByPhone(@Valid @RequestBody ResetPasswordByPhoneDTO dto) {
        userService.resetPasswordByPhone(dto);
        return Result.success("密码重置成功");
    }

    /**
     * 分页查询用户：管理员按条件查看用户列表。
     */
    @GetMapping("/page")
    @RequirePermission("user:manage")
    @Operation(summary = "分页查询用户")
    public Result<PageVO<UserInfoVO>> pageUsers(UserPageQueryDTO queryDTO) {
        return Result.success(userService.pageUsers(queryDTO));
    }

    /**
     * 更新用户状态：管理员启用或禁用某个用户。
     */
    @PutMapping("/status")
    @AdminOnly
    @Operation(summary = "修改用户状态")
    public Result<String> updateUserStatus(@Valid @RequestBody UpdateUserStatusDTO updateUserStatusDTO) {
        userService.updateUserStatus(updateUserStatusDTO);
        return Result.success("用户状态修改成功");
    }

    /**
     * 更新用户角色：管理员调整某个用户的角色。
     */
    @PutMapping("/role")
    @RequirePermission("role:assign")
    @Operation(summary = "分配用户角色")
    public Result<String> updateUserRole(@Valid @RequestBody UpdateUserRoleDTO dto) {
        userService.updateUserRole(dto);
        return Result.success("用户角色更新成功");
    }

    /**
     * 处理 listRoles 接口：接收前端请求，调用业务层后返回统一结果。
     */
    @GetMapping("/roles")
    @RequirePermission("role:view")
    @Operation(summary = "查询角色权限")
    public Result<List<RolePermissionVO>> listRoles() {
        return Result.success(userService.listRolesWithPermissions());
    }

    /**
     * 处理 context 接口：接收前端请求，调用业务层后返回统一结果。
     */
    @GetMapping("/context")
    @AdminOnly
    @Operation(summary = "打印用户上下文")
    public Result<String> context() {
        return Result.success("userId=" + UserContext.getUserId() + ", role=" + UserContext.getRole());
    }

    /**
     * 批量查询用户简要信息：给文章、评论等服务展示用户名和头像。
     */
    @PostMapping("/batch/simple")
    @Operation(summary = "批量获取用户简要信息")
    public Result<List<UserSimpleVO>> getBatchUserSimple(@RequestBody List<Long> userIds) {
        return Result.success(userService.getBatchUserSimple(userIds));
    }

    /**
     * 校验 token 是否仍然有效：常用于网关或其他服务确认登录状态。
     */
    @GetMapping("/token/validate")
    @AdminOnly
    @Operation(summary = "校验Token")
    public Result<Boolean> validateToken(@RequestParam("userId") Long userId, @RequestParam("token") String token) {
        return Result.success(userService.validateToken(userId, token));
    }

    /**
     * 强制用户下线：管理员让指定用户的 token 失效。
     */
    @PostMapping("/kickout/{userId}")
    @AdminOnly
    @Operation(summary = "踢用户下线")
    public Result<String> kickout(@PathVariable("userId") Long userId) {
        userService.kickout(userId);
        return Result.success("已将该用户踢下线");
    }

    /**
     * 刷新登录令牌：为用户生成新的访问 token。
     */
    @PostMapping("/token/refresh")
    @Operation(summary = "刷新Token")
    public Result<LoginVO> refreshToken(
            @CookieValue(value = AuthConstants.REFRESH_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response) {
        LoginVO loginVO = userService.refreshAccessToken(refreshToken);
        addCookie(response, AuthConstants.AUTH_COOKIE_NAME, loginVO.getToken(), "/", AuthConstants.AUTH_COOKIE_MAX_AGE);
        addCookie(response, AuthConstants.REFRESH_COOKIE_NAME, loginVO.getRefreshToken(),
                AuthConstants.REFRESH_COOKIE_PATH, AuthConstants.REFRESH_COOKIE_MAX_AGE);
        addCsrfCookie(response);
        loginVO.setToken(null);
        loginVO.setRefreshToken(null);
        return new Result<>(200, "Token刷新成功", loginVO);
    }

    /**
     * 处理 activitySummary 接口：接收前端请求，调用业务层后返回统一结果。
     */
    @GetMapping("/activity/summary")
    @RequirePermission("stats:view")
    @Operation(summary = "活跃用户摘要")
    public Result<ActiveUserSummaryVO> activitySummary() {
        return Result.success(userService.getActiveUserSummary());
    }

    /**
     * 处理 activityRanking 接口：接收前端请求，调用业务层后返回统一结果。
     */
    @GetMapping("/activity/ranking")
    @RequirePermission("stats:view")
    @Operation(summary = "活跃用户排行")
    public Result<List<ActiveUserVO>> activityRanking(@RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        return Result.success(userService.listActiveUsers(limit));
    }

    /**
     * 处理 addCookie 接口：接收前端请求，调用业务层后返回统一结果。
     */
    private void addCookie(HttpServletResponse response, String name, String value, String path, int maxAge) {
        String sameSite = normalizeSameSite(cookieSameSite);
        ResponseCookie cookie = ResponseCookie.from(name, value == null ? "" : value)
                .httpOnly(true)
                .secure(cookieSecure || "None".equals(sameSite))
                .sameSite(sameSite)
                .path(path)
                .maxAge(Duration.ofSeconds(maxAge))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * 处理 addCsrfCookie 接口：接收前端请求，调用业务层后返回统一结果。
     */
    private void addCsrfCookie(HttpServletResponse response) {
        addReadableCookie(response, AuthConstants.CSRF_COOKIE_NAME, UUID.randomUUID().toString(), "/",
                AuthConstants.CSRF_COOKIE_MAX_AGE);
    }

    /**
     * 处理 addReadableCookie 接口：接收前端请求，调用业务层后返回统一结果。
     */
    private void addReadableCookie(HttpServletResponse response, String name, String value, String path, int maxAge) {
        String sameSite = normalizeSameSite(cookieSameSite);
        ResponseCookie cookie = ResponseCookie.from(name, value == null ? "" : value)
                .httpOnly(false)
                .secure(cookieSecure || "None".equals(sameSite))
                .sameSite(sameSite)
                .path(path)
                .maxAge(Duration.ofSeconds(maxAge))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * 处理 normalizeSameSite 接口：接收前端请求，调用业务层后返回统一结果。
     */
    private String normalizeSameSite(String sameSite) {
        if (sameSite == null) {
            return "Lax";
        }
        return switch (sameSite.trim().toLowerCase(Locale.ROOT)) {
            case "strict" -> "Strict";
            case "none" -> "None";
            default -> "Lax";
        };
    }
}
