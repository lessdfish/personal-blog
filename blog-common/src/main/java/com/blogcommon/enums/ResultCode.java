package com.blogcommon.enums;

/**
 * ClassName:ResultCode
 * Package:com.blogcommon.enums
 * Description:
 *
 * @Author:lyp
 * @Create:2026/3/27 - 21:22
 * @Version: v1.0
 *
 */
public enum ResultCode {
    SUCCESS(200, "成功"),

    // 通用错误
    FAIL(500, "系统异常"),

    //参数空
    PARAM_NULL(2000,"参数不能为空"),
    //页码
    PARAM_NOT_NULL(2001,"分页参数不能为空"),
    PARAM_ERROR(2002,"页码不能为空且必须大于0"),
    PARAM_ERROR1(2003,"每条页数不能为空且必须大于0"),
    // 登录相关
    UNAUTHORIZED(2004, "未登录"),
    TOKEN_INVALID(2005, "token无效或已过期"),
    // 用户模块
    USER_NOT_EXIST(3001, "用户不存在"),
    USER_UPDATE_FAILED(3002,"用户信息更新失败"),
    USER_STATUS_INVALID(3003, "用户状态不合法"),
    USER_STATUS_UPDATE_FAILED(3004, "用户状态修改失败"),
    USER_DISABLED(3005, "该用户已被禁用"),

    //password
    PASSWORD_ERROR(3006, "密码错误"),
    OLD_PASSWORD_ERROR(3007,"旧密码错误"),
    NOT_SAME(3008,"新密码不能与旧密码相同"),

    PASSWORD_UPDATE_FAILED(3009,"密码修改失败"),
    USERNAME_NOT_EXIST(3010, "用户名不存在"),
    USERNAME_EXIST(3011, "用户名已存在"),

    //权限
    ROLE_NULL(3012,"角色不存在"),
    FORBIDDEN(3013,"无权限访问"),


    //文章
    ARTICLE_NOT_EXIST(4001, "文章不存在"),
    ARTICLE_PUBLISH_FAILED(4002, "文章发布失败"),
    ARTICLE_UPDATE_FAILED(4003, "文章修改失败"),
    ARTICLE_DELETE_FAILED(4004, "文章删除失败"),
    TITLE_NOT_NULL(4005,"标题不能为空"),
    CONTENT_NOT_NULL(4006,"内容不能为空"),

    //评论
    COMMENT_CREATE_FAILED(5001,"评论创建失败"),
    COMMENT_DELETE_FAILED(5002, "评论删除失败"),
    COMMENT_NOT_FOUND(5003,"评论未找到" ),
    ARTICLE_NOT_FOUND(5004, "文章未找到"),

    //通知
    NOTIFY_NOT_FOUND(6001, "通知不存在"),
    NOTIFY_READ_FAILED(6002, "通知已读标记失败"),
    NOTIFY_DELETE_FAILED(6003, "通知删除失败"),


    REDIS_NOT_RUNNING(6004, "Redis 未运行");
    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
