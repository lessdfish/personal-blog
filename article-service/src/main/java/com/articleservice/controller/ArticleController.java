package com.articleservice.controller;

import com.articleservice.config.UserContext;
import com.articleservice.dto.ArticlePublishDTO;
import com.articleservice.service.ArticleService;
import com.articleservice.vo.ArticleDetailVO;
import com.articleservice.vo.ArticleListVO;
import com.articleservice.vo.ArticlePageQueryDTO;
import com.articleservice.vo.ArticleSimpleVO;
import com.articleservice.vo.PageVO;
import com.blogcommon.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author lyp
 * @since 2026-03-28
 */
@RestController
@RequestMapping("/article")
@Tag(name = "文章模块",description = "文章相关接口")
public class ArticleController {

    @Autowired
    private ArticleService articleService;

    @PostMapping("/publish")
    @Operation(summary = "发布文章",description = "当前登录用户发布一篇新文章")
    public Result<String> publish(@Valid @RequestBody ArticlePublishDTO articlePublishDTO) {
        Long userId = UserContext.getUserId();
        articleService.publish(userId, articlePublishDTO);
        return Result.success("文章发布成功");
    }

    @GetMapping("/page")
    @Operation(summary = "文章分页",description = "文章分页")
    public Result<PageVO<ArticleListVO>> pageArticles(@Valid ArticlePageQueryDTO queryDTO) {
        PageVO<ArticleListVO> pageVO = articleService.pageArticles(queryDTO);
        return Result.success(pageVO);
    }

    @GetMapping("/detail/{id}")
    @Operation(summary = "文章细节信息",description = "根据文章ID返回文章详情")
    public Result<ArticleDetailVO> detail(@PathVariable("id") Long id) {
        return Result.success(articleService.getDetail(id));
    }

    @GetMapping("/hot")
    @Operation(summary = "热榜",description = "查看热榜前10")
    public Result<List<ArticleListVO>> hot(@RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(articleService.listHotArticles(limit));
    } //检查hot模块是否正常工作

    @GetMapping("/simple/{id}")
    @Operation(summary = "获取文章简要信息", description = "供其他服务调用，返回文章ID、作者ID、标题")
    public Result<ArticleSimpleVO> getSimpleById(@PathVariable("id") Long id) {
        return Result.success(articleService.getSimpleById(id));
    }

    // ==================== 点赞功能 ====================

    @PostMapping("/like/{articleId}")
    @Operation(summary = "点赞/取消点赞文章", description = "返回true表示点赞成功，false表示取消点赞")
    public Result<Boolean> likeArticle(@PathVariable("articleId") Long articleId) {
        Long userId = UserContext.getUserId();
        return Result.success(articleService.likeArticle(userId, articleId));
    }

    @GetMapping("/likes/{articleId}")
    @Operation(summary = "获取文章点赞数", description = "获取文章的点赞数量")
    public Result<Long> getArticleLikes(@PathVariable("articleId") Long articleId) {
        return Result.success(articleService.getArticleLikes(articleId));
    }

    @GetMapping("/liked/{articleId}")
    @Operation(summary = "检查是否已点赞", description = "检查当前用户是否已点赞该文章")
    public Result<Boolean> hasLiked(@PathVariable("articleId") Long articleId) {
        Long userId = UserContext.getUserId();
        return Result.success(articleService.hasLiked(userId, articleId));
    }

    // ==================== 浏览量功能 ====================

    @GetMapping("/views/{articleId}")
    @Operation(summary = "获取文章浏览量", description = "获取文章的浏览数量")
    public Result<Long> getArticleViews(@PathVariable("articleId") Long articleId) {
        return Result.success(articleService.getArticleViews(articleId));
    }

    // ==================== 文章编辑删除 ====================

    @PutMapping("/edit/{articleId}")
    @Operation(summary = "编辑文章", description = "编辑文章内容（带分布式锁）")
    public Result<String> editArticle(@PathVariable("articleId") Long articleId,
                                       @Valid @RequestBody ArticlePublishDTO dto) {
        Long userId = UserContext.getUserId();
        articleService.editArticle(userId, articleId, dto);
        return Result.success("文章编辑成功");
    }

    //检查文章编辑问题

    @DeleteMapping("/{articleId}")
    @Operation(summary = "删除文章", description = "软删除文章")
    public Result<String> deleteArticle(@PathVariable("articleId") Long articleId) {
        Long userId = UserContext.getUserId();
        articleService.deleteArticle(userId, articleId);
        return Result.success("文章删除成功");
    }
}
