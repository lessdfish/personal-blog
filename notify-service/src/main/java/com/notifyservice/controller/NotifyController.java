package com.notifyservice.controller;

import com.blogcommon.result.Result;
import com.notifyservice.config.UserContext;
import com.notifyservice.dto.NotifyPageQueryDTO;
import com.notifyservice.vo.NotifyListItemVO;
import com.notifyservice.service.NotifyService;
import com.notifyservice.vo.NotifyVO;
import com.notifyservice.vo.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * ClassName:NotifyController
 * Package:com.notifyservice.controller
 * Description:通知控制器
 *
 * @Author:lyp
 * @Create:2026/4/1
 * @Version: v1.0
 */
@RestController
@RequestMapping("/notify")
@Tag(name = "通知模块", description = "通知相关接口")
public class NotifyController {

    @Autowired
    private NotifyService notifyService;

    /**
     * 处理 page 接口：接收前端请求，调用业务层后返回统一结果。
     */
    @PostMapping("/page")
    @Operation(summary = "分页查询通知", description = "分页查询当前用户的通知列表")
    public Result<PageResult<NotifyListItemVO>> page(@Valid @RequestBody NotifyPageQueryDTO dto) {
        return Result.success(notifyService.pageByUser(UserContext.getUserId(), dto));
    }

    /**
     * 处理 detail 接口：接收前端请求，调用业务层后返回统一结果。
     */
    @GetMapping("/{id}")
    @Operation(summary = "通知详情", description = "查询单条通知的完整内容")
    public Result<NotifyVO> detail(@PathVariable("id") Long id) {
        return Result.success(notifyService.getDetail(UserContext.getUserId(), id));
    }

    /**
     * 处理 unreadCount 接口：接收前端请求，调用业务层后返回统一结果。
     */
    @GetMapping("/unread/count")
    @Operation(summary = "未读数量", description = "获取当前用户的未读通知数量")
    public Result<Long> unreadCount() {
        return Result.success(notifyService.getUnreadCount(UserContext.getUserId()));
    }

    /**
     * 标记单条通知已读：更新数据库并同步未读数缓存。
     */
    @PutMapping("/read/{id}")
    @Operation(summary = "标记已读", description = "标记单条通知为已读")
    public Result<Void> markAsRead(@PathVariable("id") Long id) {
        notifyService.markAsRead(UserContext.getUserId(), id);
        return Result.success();
    }

    /**
     * 标记全部通知已读：把当前用户所有未读通知改为已读。
     */
    @PutMapping("/read/all")
    @Operation(summary = "全部已读", description = "标记所有通知为已读")
    public Result<Void> markAllAsRead() {
        notifyService.markAllAsRead(UserContext.getUserId());
        return Result.success();
    }

    /**
     * 删除数据：校验权限后删除或标记删除指定记录。
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除通知", description = "删除单条通知")
    public Result<Void> delete(@PathVariable("id") Long id) {
        notifyService.delete(UserContext.getUserId(), id);
        return Result.success();
    }
}
