package com.articleservice.controller;

import com.articleservice.config.UserContext;
import com.articleservice.dto.ArticlePublishDTO;
import com.articleservice.service.ArticleService;
import com.articleservice.vo.ArticleDetailVO;
import com.articleservice.vo.ArticleListVO;
import com.articleservice.vo.ArticlePageQueryDTO;
import com.articleservice.vo.PageVO;
import com.blogcommon.result.Result;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;

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
public class ArticleController {

    @Autowired
    private ArticleService articleService;

    @PostMapping("/publish")
    public Result<String> publish(@Valid @RequestBody ArticlePublishDTO articlePublishDTO) {
        Long userId = UserContext.getUserId();
        articleService.publish(userId, articlePublishDTO);
        return Result.success("文章发布成功");
    }

    @GetMapping("/page")
    public Result<PageVO<ArticleListVO>> pageArticles(@Valid ArticlePageQueryDTO queryDTO) {
        PageVO<ArticleListVO> pageVO = articleService.pageArticles(queryDTO);
        return Result.success(pageVO);
    }

    @GetMapping("/detail/{id}")
    public Result<ArticleDetailVO> detail(@PathVariable Long id) {
        return Result.success(articleService.getDetail(id));
    }

    @GetMapping("/hot")
    public Result<List<ArticleListVO>> hot(@RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(articleService.listHotArticles(limit));
    }
}
