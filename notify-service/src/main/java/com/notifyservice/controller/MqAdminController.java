package com.notifyservice.controller;

import com.blogcommon.enums.ResultCode;
import com.blogcommon.exception.BusinessException;
import com.blogcommon.result.Result;
import com.notifyservice.config.UserContext;
import com.notifyservice.service.MqAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notify/admin/mq")
@Tag(name = "MQ运维", description = "通知服务消息队列运维接口")
public class MqAdminController {
    private final MqAdminService mqAdminService;

    /**
     * 构造 MqAdminController：注入这个类运行时需要的依赖。
     */
    public MqAdminController(MqAdminService mqAdminService) {
        this.mqAdminService = mqAdminService;
    }

    @GetMapping("/dlq")
    @Operation(summary = "查看死信队列概览")
    public Result<MqAdminService.DlqOverview> getDlqOverview() {
        requireAdmin();
        return Result.success(mqAdminService.getDlqOverview());
    }

    @PostMapping("/dlq/{name}/requeue")
    @Operation(summary = "重投死信队列消息")
    public Result<MqAdminService.RequeueResult> requeue(@PathVariable("name") String name,
                                                        @RequestParam(value = "count", defaultValue = "10") Integer count) {
        requireAdmin();
        return Result.success(mqAdminService.requeue(name, count));
    }

    /**
     * 校验当前用户是否管理员，不是管理员就抛出无权限异常。
     */
    private void requireAdmin() {
        if (!"ADMIN".equals(UserContext.getRole())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }
}
