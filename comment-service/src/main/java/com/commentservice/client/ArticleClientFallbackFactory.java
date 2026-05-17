package com.commentservice.client;

import com.blogcommon.result.Result;
import com.commentservice.vo.ArticleSimpleVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ArticleClientFallbackFactory implements FallbackFactory<ArticleClient> {
    @Override
    public ArticleClient create(Throwable cause) {
        return new ArticleClient() {
            @Override
            public Result<ArticleSimpleVO> getSimpleById(Long id) {
                log.warn("article-service文章简要信息查询降级, articleId={}", id, cause);
                return Result.fail(500, "文章服务暂不可用");
            }

            /**
             * 远程调用 updateCommentCount：通过 Feign 调用其他微服务接口。
             */
            @Override
            public Result<Void> updateCommentCount(Long articleId, Integer delta) {
                log.warn("article-service评论数同步降级, articleId={}, delta={}", articleId, delta, cause);
                return Result.success();
            }
        };
    }
}
