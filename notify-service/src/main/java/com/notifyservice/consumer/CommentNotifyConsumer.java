package com.notifyservice.consumer;

import com.blogcommon.message.CommentNotifyMessage;
import com.blogcommon.message.MqConstants;
import com.notifyservice.service.NotifyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * ClassName:CommentNotifyConsumer
 * Package:com.notifyservice.consumer
 * Description:评论通知消费者
 *
 * @Author:lyp
 * @Create:2026/4/1
 * @Version: v1.0
 */
@Slf4j
@Component
public class CommentNotifyConsumer {

    @Autowired
    private NotifyService notifyService;

    @RabbitListener(queues = MqConstants.COMMENT_NOTIFY_QUEUE)
    public void handleCommentNotify(CommentNotifyMessage message) {
        log.info("收到评论通知消息: {}", message);
        try {
            notifyService.handleCommentNotify(message);
            log.info("评论通知处理成功, receiverId={}", message.getReceiverId());
        } catch (Exception e) {
            log.error("评论通知处理失败: {}", e.getMessage(), e);
        }
    }
}
