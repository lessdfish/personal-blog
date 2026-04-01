package com.commentservice.client;

import com.blogcommon.result.Result;
import com.commentservice.config.FeignConfig;
import com.commentservice.vo.ArticleSimpleVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * ClassName:ArticleClient
 * Package:com.commentservice.client
 * Description:文章服务Feign客户端
 *
 * @Author:lyp
 * @Create:2026/4/1 - 00:18
 * @Version: v1.0
 */
@FeignClient(name = "article-service", url = "${feign.article-service.url}", configuration = FeignConfig.class)
public interface ArticleClient {
    
    /**
     * 获取文章简要信息
     * @param id 文章ID
     * @return 文章简要信息
     */
    @GetMapping("/article/simple/{id}")
    Result<ArticleSimpleVO> getSimpleById(@PathVariable("id") Long id);
}
