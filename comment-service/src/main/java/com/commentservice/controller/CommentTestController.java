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

/**
 * ClassName:CommentTestController
 * Package:com.commentservice.controller
 * Description:
 *
 * @Author:lyp
 * @Create:2026/3/26 - 22:11
 * @Version: v1.0
 *
 */
@Tag(name = "评论模块")
@RestController
@RequestMapping("/comment")
public class CommentTestController {

    @Autowired
    private CommentService commentService;

    @PostMapping
    @Operation(summary = "创建评论", description = "支持文章评论和回复评论")
    public Result<Long> create(@Valid @RequestBody CommentCreateDTO dto){
        return Result.success(commentService.create(UserContext.getUserId(),dto));
    }
    //文章未找到

    @GetMapping("/article/{articleId}")
    @Operation(summary = "创建评论", description = "支持文章评论和回复评论")
    public Result<List<CommentVO>> listByArticleId(@PathVariable("articleId") Long articleId) {
        return Result.success(commentService.listByArticleId(articleId));
    }

    @Operation(summary = "分页查询评论", description = "按文章分页查询评论树，返回一级评论及其回复")
    @PostMapping("/page")
    public Result<PageResult<CommentVO>> page(@Valid @RequestBody CommentPageQueryDTO dto) {
        return Result.success(commentService.pageByArticle(dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除评论", description = "根据id删除评论")
    public Result<Void> delete(@PathVariable("id") Long id){
        commentService.delete(UserContext.getUserId(),id);
        return Result.success();
    }

    // ==================== 限流功能 ====================

    @GetMapping("/rate-limit/remaining")
    @Operation(summary = "获取剩余评论次数", description = "获取当前用户在限流窗口内的剩余评论次数")
    public Result<Integer> getRemainingComments() {
        Long userId = UserContext.getUserId();
        return Result.success(commentService.getRemainingComments(userId));
    }
}
