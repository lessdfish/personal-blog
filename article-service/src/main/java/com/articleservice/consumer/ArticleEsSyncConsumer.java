package com.articleservice.consumer;

import com.articleservice.entity.Article;
import com.articleservice.mapper.ArticleMapper;
import com.articleservice.service.ArticleSearchService;
import com.blogcommon.message.ArticleEsSyncMessage;
import com.blogcommon.message.MqConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ArticleEsSyncConsumer {
    private final ArticleMapper articleMapper;
    private final ArticleSearchService articleSearchService;

    /**
     * 构造 ES 同步消费者：注入查文章的 Mapper 和操作搜索索引的 Service。
     */
    public ArticleEsSyncConsumer(ArticleMapper articleMapper, ArticleSearchService articleSearchService) {
        this.articleMapper = articleMapper;
        this.articleSearchService = articleSearchService;
    }

    /**
     * 处理文章搜索同步消息：文章新增/修改就写入 ES，文章删除或下架就从 ES 删除。
     */
    @RabbitListener(queues = MqConstants.ARTICLE_ES_SYNC_QUEUE)
    public void handleArticleEsSync(ArticleEsSyncMessage message) {
        if (message == null || message.getArticleId() == null) {
            return;
        }
        if (ArticleEsSyncMessage.ACTION_DELETE.equals(message.getAction())) {
            articleSearchService.delete(message.getArticleId());
            return;
        }
        Article article = articleMapper.selectAnyById(message.getArticleId());
        if (article == null || !Integer.valueOf(1).equals(article.getStatus())) {
            articleSearchService.delete(message.getArticleId());
            return;
        }
        articleSearchService.save(article);
        log.info("文章ES索引同步完成, articleId={}", message.getArticleId());
    }
}
