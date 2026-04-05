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

    @PostMapping
    @Operation(summary = "创建评论", description = "支持文章评论和回复评论")
    public Result<Long> create(@Valid @RequestBody CommentCreateDTO dto) {
        return Result.success(commentService.create(UserContext.getUserId(), dto));
    }

    @GetMapping("/article/{articleId}")
    @Operation(summary = "查询文章评论")
    public Result<List<CommentVO>> listByArticleId(@PathVariable("articleId") Long articleId) {
        return Result.success(commentService.listByArticleId(articleId));
    }

    @PostMapping("/page")
    @Operation(summary = "分页查询评论树")
    public Result<PageResult<CommentVO>> page(@Valid @RequestBody CommentPageQueryDTO dto) {
        return Result.success(commentService.pageByArticle(dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除评论", description = "作者可删自己的评论，管理员和版主可删任意评论")
    public Result<Void> delete(@PathVariable("id") Long id) {
        commentService.delete(UserContext.getUserId(), UserContext.getRole(), id);
        return Result.success();
    }

    @GetMapping("/rate-limit/remaining")
    @Operation(summary = "剩余评论次数")
    public Result<Integer> getRemainingComments() {
        return Result.success(commentService.getRemainingComments(UserContext.getUserId()));
    }
}
