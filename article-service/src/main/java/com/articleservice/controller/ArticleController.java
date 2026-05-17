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

    /**
     * 发布帖子接口：把前端提交的帖子内容交给业务层保存。
     */
    @PostMapping("/publish")
    @Operation(summary = "发布帖子")
    public Result<String> publish(@Valid @RequestBody ArticlePublishDTO articlePublishDTO) {
        articleService.publish(UserContext.getUserId(), articlePublishDTO);
        return Result.success("帖子发布成功");
    }

    /**
     * 分页查询普通帖子接口：目前等同于 pageNormalArticles，保留给旧接口路径使用。
     */
    @GetMapping("/page")
    @Operation(summary = "普通帖子分页")
    public Result<PageVO<ArticleListVO>> pageArticles(@Valid ArticlePageQueryDTO queryDTO) {
        return Result.success(articleService.pageNormalArticles(queryDTO));
    }

    /**
     * 分页查询普通帖子接口：按页码、版块、作者、关键词等条件查询文章列表。
     */
    @GetMapping("/page/normal")
    @Operation(summary = "普通帖子分页")
    public Result<PageVO<ArticleListVO>> pageNormalArticles(@Valid ArticlePageQueryDTO queryDTO) {
        return Result.success(articleService.pageNormalArticles(queryDTO));
    }

    /**
     * 分页查询热榜接口：按热度顺序返回文章列表。
     */
    @GetMapping("/page/hot")
    @Operation(summary = "热榜分页")
    public Result<PageVO<ArticleListVO>> pageHotArticles(@RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                                         @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        return Result.success(articleService.pageHotArticles(pageNum, pageSize));
    }

    /**
     * 查询帖子详情接口：读取正文内容，并记录当前用户或 IP 的浏览行为。
     */
    @GetMapping("/detail/{id}")
    @Operation(summary = "帖子详情")
    public Result<ArticleDetailVO> detail(@PathVariable("id") Long id, HttpServletRequest request) {
        String viewerKey = UserContext.getUserId() != null ? "u:" + UserContext.getUserId() : "ip:" + request.getRemoteAddr();
        return Result.success(articleService.getDetail(id, viewerKey));
    }

    /**
     * 查询热榜接口：返回指定数量的热门文章，常用于首页侧边栏。
     */
    @GetMapping("/hot")
    @Operation(summary = "热榜")
    public Result<List<ArticleListVO>> hot(@RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        return Result.success(articleService.listHotArticles(limit));
    }

    /**
     * 查询帖子简要信息接口：给其他服务使用，只返回 id、标题、作者、是否允许评论等基础字段。
     */
    @GetMapping("/simple/{id}")
    @Operation(summary = "获取帖子简要信息")
    public Result<ArticleSimpleVO> getSimpleById(@PathVariable("id") Long id) {
        return Result.success(articleService.getSimpleById(id));
    }

    /**
     * 点赞接口：把当前登录用户对文章的状态设置为已点赞。
     */
    @PutMapping("/like/{articleId}")
    @Operation(summary = "显式点赞")
    public Result<Boolean> likeArticle(@PathVariable("articleId") Long articleId) {
        return Result.success(articleService.setArticleLikeStatus(UserContext.getUserId(), articleId, true));
    }

    /**
     * 取消点赞接口：把当前登录用户对文章的状态设置为未点赞。
     */
    @DeleteMapping("/like/{articleId}")
    @Operation(summary = "显式取消点赞")
    public Result<Boolean> unlikeArticle(@PathVariable("articleId") Long articleId) {
        return Result.success(articleService.setArticleLikeStatus(UserContext.getUserId(), articleId, false));
    }

    /**
     * 查询点赞数接口：返回某篇文章现在有多少个点赞。
     */
    @GetMapping("/likes/{articleId}")
    @Operation(summary = "点赞数")
    public Result<Long> getArticleLikes(@PathVariable("articleId") Long articleId) {
        return Result.success(articleService.getArticleLikes(articleId));
    }

    /**
     * 查询是否已点赞接口：判断当前登录用户是否点赞过这篇文章。
     */
    @GetMapping("/liked/{articleId}")
    @Operation(summary = "是否已点赞")
    public Result<Boolean> hasLiked(@PathVariable("articleId") Long articleId) {
        return Result.success(articleService.hasLiked(UserContext.getUserId(), articleId));
    }

    /**
     * 收藏接口：把当前登录用户对文章的状态设置为已收藏。
     */
    @PutMapping("/favorite/{articleId}")
    @Operation(summary = "显式收藏")
    public Result<Boolean> favorite(@PathVariable("articleId") Long articleId) {
        return Result.success(articleService.setArticleFavoriteStatus(UserContext.getUserId(), articleId, true));
    }

    /**
     * 取消收藏接口：把当前登录用户对文章的状态设置为未收藏。
     */
    @DeleteMapping("/favorite/{articleId}")
    @Operation(summary = "显式取消收藏")
    public Result<Boolean> unfavorite(@PathVariable("articleId") Long articleId) {
        return Result.success(articleService.setArticleFavoriteStatus(UserContext.getUserId(), articleId, false));
    }

    /**
     * 查询是否已收藏接口：判断当前登录用户是否收藏过这篇文章。
     */
    @GetMapping("/favorited/{articleId}")
    @Operation(summary = "是否已收藏")
    public Result<Boolean> hasFavorited(@PathVariable("articleId") Long articleId) {
        return Result.success(articleService.hasFavorited(UserContext.getUserId(), articleId));
    }

    /**
     * 我的收藏分页接口：返回当前登录用户收藏过的文章列表。
     */
    @GetMapping("/favorites")
    @Operation(summary = "我的收藏")
    public Result<PageVO<ArticleListVO>> favorites(@RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                                   @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        return Result.success(articleService.pageMyFavorites(UserContext.getUserId(), pageNum, pageSize));
    }

    /**
     * 查询收藏数接口：返回某篇文章现在被收藏了多少次。
     */
    @GetMapping("/favorites/count/{articleId}")
    @Operation(summary = "收藏数")
    public Result<Long> getArticleFavorites(@PathVariable("articleId") Long articleId) {
        return Result.success(articleService.getArticleFavorites(articleId));
    }

    /**
     * 查询浏览量接口：返回某篇文章当前记录的浏览次数。
     */
    @GetMapping("/views/{articleId}")
    @Operation(summary = "浏览量")
    public Result<Long> getArticleViews(@PathVariable("articleId") Long articleId) {
        return Result.success(articleService.getArticleViews(articleId));
    }

    /**
     * 查询热度接口：返回某篇文章的综合热度分数。
     */
    @GetMapping("/heat/{articleId}")
    @Operation(summary = "帖子热度")
    public Result<Double> getArticleHeat(@PathVariable("articleId") Long articleId) {
        return Result.success(articleService.getHeat(articleId));
    }

    /**
     * 编辑帖子接口：作者或管理员可以修改文章标题、摘要、正文、版块和标签。
     */
    @PutMapping("/edit/{articleId}")
    @Operation(summary = "编辑帖子")
    public Result<String> editArticle(@PathVariable("articleId") Long articleId, @Valid @RequestBody ArticlePublishDTO dto) {
        articleService.editArticle(UserContext.getUserId(), UserContext.getRole(), articleId, dto);
        return Result.success("帖子编辑成功");
    }

    /**
     * 删除帖子接口：作者或管理员可以把文章状态改为删除。
     */
    @DeleteMapping("/{articleId}")
    @Operation(summary = "删除帖子")
    public Result<String> deleteArticle(@PathVariable("articleId") Long articleId) {
        articleService.deleteArticle(UserContext.getUserId(), UserContext.getRole(), articleId);
        return Result.success("帖子删除成功");
    }

    /**
     * 管理帖子接口：管理员可以设置置顶、精华、是否允许评论和文章状态。
     */
    @PutMapping("/manage/{articleId}")
    @Operation(summary = "帖子运营管理")
    public Result<String> manageArticle(@PathVariable("articleId") Long articleId, @RequestBody ArticleManageDTO dto) {
        articleService.manageArticle(UserContext.getRole(), articleId, dto);
        return Result.success("帖子状态更新成功");
    }

    /**
     * 创建版块接口：管理员新增一个文章版块。
     */
    @PostMapping("/board")
    @Operation(summary = "创建版块")
    public Result<String> createBoard(@Valid @RequestBody BoardCreateDTO dto) {
        articleService.createBoard(UserContext.getRole(), dto);
        return Result.success("版块创建成功");
    }

    /**
     * 查询版块列表接口：返回所有启用中的版块，给发帖页和列表筛选使用。
     */
    @GetMapping("/board/list")
    @Operation(summary = "版块列表")
    public Result<List<BoardVO>> listBoards() {
        return Result.success(articleService.listBoards());
    }

    /**
     * 内部更新评论数接口：评论服务新增或删除评论后，调用这里同步文章评论数。
     */
    @PostMapping("/comment/count/{articleId}/incr")
    @Operation(summary = "内部接口：更新评论数")
    public Result<Void> updateCommentCount(@PathVariable("articleId") Long articleId,
                                           @RequestParam(value = "delta", defaultValue = "1") Integer delta) {
        articleService.updateArticleCommentCount(articleId, delta);
        return Result.success();
    }
}
