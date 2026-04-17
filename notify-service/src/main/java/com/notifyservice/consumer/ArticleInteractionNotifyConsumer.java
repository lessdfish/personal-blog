package com.notifyservice.consumer;

import com.blogcommon.message.ArticleInteractionNotifyMessage;
import com.blogcommon.message.MqConstants;
import com.notifyservice.service.NotifyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ArticleInteractionNotifyConsumer {

    @Autowired
    private NotifyService notifyService;

    @RabbitListener(queues = MqConstants.ARTICLE_INTERACTION_NOTIFY_QUEUE)
    public void handleInteractionNotify(ArticleInteractionNotifyMessage message) {
        log.info("收到文章互动通知消息: {}", message);
        try {
            notifyService.handleArticleInteractionNotify(message);
            log.info("文章互动通知处理成功, receiverId={}", message.getReceiverId());
        } catch (Exception e) {
            log.error("文章互动通知处理失败: {}", e.getMessage(), e);
        }
    }
}
