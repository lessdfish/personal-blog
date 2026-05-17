package com.commentservice.controller;

import com.blogcommon.result.Result;
import com.commentservice.config.UserContext;
import com.commentservice.dto.CommentCreateDTO;
import com.commentservice.dto.CommentPageQueryDTO;
import com.commentservice.service.CommentService;
import com.commentservice.vo.CommentVO;
import com.commentservice.vo.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "评论模块")
@RestController
@RequestMapping("/comment")
public class CommentTestController {

    @Autowired
    private CommentService commentService;

    /**
     * 创建数据：接收请求参数，校验后保存一条新记录。
     */
    @PostMapping
    @Operation(summary = "创建评论", description = "支持文章评论和回复评论")
    public Result<Long> create(@Valid @RequestBody CommentCreateDTO dto) {
        return Result.success(commentService.create(UserContext.getUserId(), dto));
    }

    /**
     * 查询文章评论列表：按文章 id 获取评论及其回复。
     */
    @GetMapping("/article/{articleId}")
    @Operation(summary = "查询文章评论")
    public Result<List<CommentVO>> listByArticleId(@PathVariable("articleId") Long articleId) {
        return Result.success(commentService.listByArticleId(articleId));
    }

    /**
     * 处理 page 接口：接收前端请求，调用业务层后返回统一结果。
     */
    @PostMapping("/page")
    @Operation(summary = "分页查询评论树")
    public Result<PageResult<CommentVO>> page(@Valid @RequestBody CommentPageQueryDTO dto) {
        return Result.success(commentService.pageByArticle(dto));
    }

    /**
     * 删除数据：校验权限后删除或标记删除指定记录。
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除评论", description = "作者可删自己的评论，管理员和版主可删任意评论")
    public Result<Void> delete(@PathVariable("id") Long id) {
        commentService.delete(UserContext.getUserId(), UserContext.getRole(), id);
        return Result.success();
    }

    /**
     * 查询剩余可评论次数：用于告诉用户当前频率限制还剩多少。
     */
    @GetMapping("/rate-limit/remaining")
    @Operation(summary = "剩余评论次数")
    public Result<Integer> getRemainingComments() {
        return Result.success(commentService.getRemainingComments(UserContext.getUserId()));
    }
}
