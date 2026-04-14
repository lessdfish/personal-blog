package com.commentservice.client;

import com.blogcommon.result.Result;
import com.commentservice.config.FeignConfig;
import com.commentservice.vo.ArticleSimpleVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "article-service", configuration = FeignConfig.class)
public interface ArticleClient {
    @GetMapping("/article/simple/{id}")
    Result<ArticleSimpleVO> getSimpleById(@PathVariable("id") Long id);

    @PostMapping("/article/comment/count/{articleId}/incr")
    Result<Void> updateCommentCount(@PathVariable("articleId") Long articleId, @RequestParam("delta") Integer delta);
}
