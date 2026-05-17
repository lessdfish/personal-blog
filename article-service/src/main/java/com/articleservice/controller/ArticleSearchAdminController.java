package com.articleservice.controller;

import com.articleservice.config.UserContext;
import com.articleservice.service.ArticleSearchService;
import com.blogcommon.enums.ResultCode;
import com.blogcommon.exception.BusinessException;
import com.blogcommon.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/article/admin/search")
@Tag(name = "搜索运维", description = "文章搜索索引运维接口")
public class ArticleSearchAdminController {
    private final ArticleSearchService articleSearchService;

    /**
     * 构造搜索运维接口：注入文章搜索服务。
     */
    public ArticleSearchAdminController(ArticleSearchService articleSearchService) {
        this.articleSearchService = articleSearchService;
    }

    /**
     * 管理员手动重建搜索索引：把数据库里的文章重新写入 Elasticsearch。
     */
    @PostMapping("/reindex")
    @Operation(summary = "重建文章搜索索引")
    public Result<ArticleSearchService.ReindexResult> reindex(@RequestParam(value = "pageSize", defaultValue = "100") Integer pageSize) {
        if (!"ADMIN".equals(UserContext.getRole())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return Result.success(articleSearchService.reindexAll(pageSize == null ? 100 : pageSize));
    }
}
