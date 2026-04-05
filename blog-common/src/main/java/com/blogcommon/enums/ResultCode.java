package com.blogcommon.enums;

public enum ResultCode {
    SUCCESS(200, "成功"),
    FAIL(500, "系统异常"),

    PARAM_NULL(2000, "参数不能为空"),
    PARAM_NOT_NULL(2001, "分页参数不能为空"),
    PARAM_ERROR(2002, "页码不能为空且必须大于0"),
    PARAM_ERROR1(2003, "每页条数不能为空且必须大于0"),
    UNAUTHORIZED(2004, "未登录"),
    TOKEN_INVALID(2005, "token无效或已过期"),
    GATEWAY_ROUTE_ERROR(2006, "网关路由配置异常"),
    GATEWAY_AUTH_ERROR(2007, "网关鉴权失败"),

    USER_NOT_EXIST(3001, "用户不存在"),
    USER_UPDATE_FAILED(3002, "用户信息更新失败"),
    USER_STATUS_INVALID(3003, "用户状态不合法"),
    USER_STATUS_UPDATE_FAILED(3004, "用户状态修改失败"),
    USER_DISABLED(3005, "该用户已被禁用"),
    PASSWORD_ERROR(3006, "密码错误"),
    OLD_PASSWORD_ERROR(3007, "旧密码错误"),
    NOT_SAME(3008, "新密码不能与旧密码相同"),
    PASSWORD_UPDATE_FAILED(3009, "密码修改失败"),
    USERNAME_NOT_EXIST(3010, "用户名不存在"),
    USERNAME_EXIST(3011, "用户名已存在"),
    ROLE_NULL(3012, "角色不存在"),
    FORBIDDEN(3013, "无权限访问"),
    USER_ROLE_UPDATE_FAILED(3014, "用户角色更新失败"),
    PERMISSION_DENIED(3015, "权限点校验未通过"),

    ARTICLE_NOT_EXIST(4001, "帖子不存在"),
    ARTICLE_PUBLISH_FAILED(4002, "帖子发布失败"),
    ARTICLE_UPDATE_FAILED(4003, "帖子修改失败"),
    ARTICLE_DELETE_FAILED(4004, "帖子删除失败"),
    TITLE_NOT_NULL(4005, "标题不能为空"),
    CONTENT_NOT_NULL(4006, "内容不能为空"),
    BOARD_NOT_EXIST(4007, "版块不存在"),
    BOARD_CODE_EXIST(4008, "版块编码已存在"),
    ARTICLE_EDIT_LOCKED(4009, "帖子正在编辑中，请稍后重试"),
    ARTICLE_COMMENT_CLOSED(4010, "该帖子已关闭评论"),
    ARTICLE_FAVORITE_FAILED(4011, "帖子收藏操作失败"),

    COMMENT_CREATE_FAILED(5001, "评论创建失败"),
    COMMENT_DELETE_FAILED(5002, "评论删除失败"),
    COMMENT_NOT_FOUND(5003, "评论未找到"),
    ARTICLE_NOT_FOUND(5004, "帖子未找到"),
    COMMENT_RATE_LIMIT(5005, "评论过于频繁，请稍后再试"),

    NOTIFY_NOT_FOUND(6001, "通知不存在"),
    NOTIFY_READ_FAILED(6002, "通知已读标记失败"),
    NOTIFY_DELETE_FAILED(6003, "通知删除失败"),
    REDIS_NOT_RUNNING(6004, "Redis 未运行"),
    MQ_SEND_FAILED(6005, "消息发送失败");

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
