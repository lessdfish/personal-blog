package com.articleservice.controller;

import com.articleservice.config.UserContext;
import com.articleservice.dto.ArticleManageDTO;
import com.articleservice.dto.ArticlePublishDTO;
import com.articleservice.dto.BoardCreateDTO;
import com.articleservice.service.ArticleService;
import com.articleservice.vo.ArticleDetailVO;
import com.articleservice.vo.ArticleListVO;
import com.articleservice.vo.ArticlePageQueryDTO;
import com.articleservice.vo.ArticleSimpleVO;
import com.articleservice.vo.BoardVO;
import com.articleservice.vo.PageVO;
import com.blogcommon.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/article")
@Tag(name = "帖子模块", description = "帖子、版块、热榜与互动接口")
public class ArticleController {

    @Autowired
    private ArticleService articleService;

    @PostMapping("/publish")
    @Operation(summary = "发布帖子")
    public Result<String> publish(@Valid @RequestBody ArticlePublishDTO articlePublishDTO) {
        articleService.publish(UserContext.getUserId(), articlePublishDTO);
        return Result.success("帖子发布成功");
    }

    @GetMapping("/page")
    @Operation(summary = "普通帖子分页")
    public Result<PageVO<ArticleListVO>> pageArticles(@Valid ArticlePageQueryDTO queryDTO) {
        return Result.success(articleService.pageNormalArticles(queryDTO));
    }

    @GetMapping("/page/normal")
    @Operation(summary = "普通帖子分页")
    public Result<PageVO<ArticleListVO>> pageNormalArticles(@Valid ArticlePageQueryDTO queryDTO) {
        return Result.success(articleService.pageNormalArticles(queryDTO));
    }

    @GetMapping("/page/hot")
    @Operation(summary = "热榜分页")
    public Result<PageVO<ArticleListVO>> pageHotArticles(@RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                                         @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        return Result.success(articleService.pageHotArticles(pageNum, pageSize));
    }

    @GetMapping("/detail/{id}")
    @Operation(summary = "帖子详情")
    public Result<ArticleDetailVO> detail(@PathVariable("id") Long id, HttpServletRequest request) {
        String viewerKey = UserContext.getUserId() != null ? "u:" + UserContext.getUserId() : "ip:" + request.getRemoteAddr();
        return Result.success(articleService.getDetail(id, viewerKey));
    }

    @GetMapping("/hot")
    @Operation(summary = "热榜")
    public Result<List<ArticleListVO>> hot(@RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        return Result.success(articleService.listHotArticles(limit));
    }

    @GetMapping("/simple/{id}")
    @Operation(summary = "获取帖子简要信息")
    public Result<ArticleSimpleVO> getSimpleById(@PathVariable("id") Long id) {
        return Result.success(articleService.getSimpleById(id));
    }

    @PutMapping("/like/{articleId}")
    @Operation(summary = "显式点赞")
    public Result<Boolean> likeArticle(@PathVariable("articleId") Long articleId) {
        return Result.success(articleService.setArticleLikeStatus(UserContext.getUserId(), articleId, true));
    }

    @DeleteMapping("/like/{articleId}")
    @Operation(summary = "显式取消点赞")
    public Result<Boolean> unlikeArticle(@PathVariable("articleId") Long articleId) {
        return Result.success(articleService.setArticleLikeStatus(UserContext.getUserId(), articleId, false));
    }

    @GetMapping("/likes/{articleId}")
    @Operation(summary = "点赞数")
    public Result<Long> getArticleLikes(@PathVariable("articleId") Long articleId) {
        return Result.success(articleService.getArticleLikes(articleId));
    }

    @GetMapping("/liked/{articleId}")
    @Operation(summary = "是否已点赞")
    public Result<Boolean> hasLiked(@PathVariable("articleId") Long articleId) {
        return Result.success(articleService.hasLiked(UserContext.getUserId(), articleId));
    }

    @PutMapping("/favorite/{articleId}")
    @Operation(summary = "显式收藏")
    public Result<Boolean> favorite(@PathVariable("articleId") Long articleId) {
        return Result.success(articleService.setArticleFavoriteStatus(UserContext.getUserId(), articleId, true));
    }

    @DeleteMapping("/favorite/{articleId}")
    @Operation(summary = "显式取消收藏")
    public Result<Boolean> unfavorite(@PathVariable("articleId") Long articleId) {
        return Result.success(articleService.setArticleFavoriteStatus(UserContext.getUserId(), articleId, false));
    }

    @GetMapping("/favorited/{articleId}")
    @Operation(summary = "是否已收藏")
    public Result<Boolean> hasFavorited(@PathVariable("articleId") Long articleId) {
        return Result.success(articleService.hasFavorited(UserContext.getUserId(), articleId));
    }

    @GetMapping("/favorites")
    @Operation(summary = "我的收藏")
    public Result<PageVO<ArticleListVO>> favorites(@RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                                   @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        return Result.success(articleService.pageMyFavorites(UserContext.getUserId(), pageNum, pageSize));
    }

    @GetMapping("/favorites/count/{articleId}")
    @Operation(summary = "收藏数")
    public Result<Long> getArticleFavorites(@PathVariable("articleId") Long articleId) {
        return Result.success(articleService.getArticleFavorites(articleId));
    }

    @GetMapping("/views/{articleId}")
    @Operation(summary = "浏览量")
    public Result<Long> getArticleViews(@PathVariable("articleId") Long articleId) {
        return Result.success(articleService.getArticleViews(articleId));
    }

    @GetMapping("/heat/{articleId}")
    @Operation(summary = "帖子热度")
    public Result<Double> getArticleHeat(@PathVariable("articleId") Long articleId) {
        return Result.success(articleService.getHeat(articleId));
    }

    @PutMapping("/edit/{articleId}")
    @Operation(summary = "编辑帖子")
    public Result<String> editArticle(@PathVariable("articleId") Long articleId, @Valid @RequestBody ArticlePublishDTO dto) {
        articleService.editArticle(UserContext.getUserId(), UserContext.getRole(), articleId, dto);
        return Result.success("帖子编辑成功");
    }

    @DeleteMapping("/{articleId}")
    @Operation(summary = "删除帖子")
    public Result<String> deleteArticle(@PathVariable("articleId") Long articleId) {
        articleService.deleteArticle(UserContext.getUserId(), UserContext.getRole(), articleId);
        return Result.success("帖子删除成功");
    }

    @PutMapping("/manage/{articleId}")
    @Operation(summary = "帖子运营管理")
    public Result<String> manageArticle(@PathVariable("articleId") Long articleId, @RequestBody ArticleManageDTO dto) {
        articleService.manageArticle(UserContext.getRole(), articleId, dto);
        return Result.success("帖子状态更新成功");
    }

    @PostMapping("/board")
    @Operation(summary = "创建版块")
    public Result<String> createBoard(@Valid @RequestBody BoardCreateDTO dto) {
        articleService.createBoard(UserContext.getRole(), dto);
        return Result.success("版块创建成功");
    }

    @GetMapping("/board/list")
    @Operation(summary = "版块列表")
    public Result<List<BoardVO>> listBoards() {
        return Result.success(articleService.listBoards());
    }

    @PostMapping("/comment/count/{articleId}/incr")
    @Operation(summary = "内部接口：更新评论数")
    public Result<Void> updateCommentCount(@PathVariable("articleId") Long articleId,
                                           @RequestParam(value = "delta", defaultValue = "1") Integer delta) {
        articleService.updateArticleCommentCount(articleId, delta);
        return Result.success();
    }
}
