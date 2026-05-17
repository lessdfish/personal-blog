package com.notifyservice.consumer;

import com.blogcommon.message.ArticleInteractionNotifyMessage;
import com.blogcommon.message.MqConstants;
import com.notifyservice.service.NotifyService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ArticleInteractionNotifyConsumer {
    private static final long MAX_RETRY_COUNT = 3L;

    @Autowired
    private NotifyService notifyService;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 消费消息 handleInteractionNotify：收到 RabbitMQ 消息后执行业务处理。
     */
    @RabbitListener(queues = MqConstants.ARTICLE_INTERACTION_NOTIFY_QUEUE)
    public void handleInteractionNotify(ArticleInteractionNotifyMessage message, Message rawMessage, Channel channel,
                                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.info("收到文章互动通知消息: {}", message);
        try {
            notifyService.handleArticleInteractionNotify(message);
            channel.basicAck(deliveryTag, false);
            log.info("文章互动通知处理成功, receiverId={}", message.getReceiverId());
        } catch (Exception e) {
            log.error("文章互动通知处理失败: {}", e.getMessage(), e);
            rejectOrDeadLetter(message, rawMessage, channel, deliveryTag);
        }
    }

    /**
     * 消费消息 rejectOrDeadLetter：收到 RabbitMQ 消息后执行业务处理。
     */
    private void rejectOrDeadLetter(ArticleInteractionNotifyMessage message, Message rawMessage, Channel channel,
                                    long deliveryTag) throws IOException {
        if (getRetryCount(rawMessage) >= MAX_RETRY_COUNT) {
            rabbitTemplate.convertAndSend(
                    MqConstants.ARTICLE_INTERACTION_NOTIFY_DLX,
                    MqConstants.ARTICLE_INTERACTION_NOTIFY_DLQ_ROUTING_KEY,
                    message);
            channel.basicAck(deliveryTag, false);
            log.warn("文章互动通知超过最大重试次数，已进入DLQ, receiverId={}", message.getReceiverId());
            return;
        }
        channel.basicReject(deliveryTag, false);
    }

    /**
     * 获取 retryCount：返回当前对象里保存的这个值。
     */
    private long getRetryCount(Message rawMessage) {
        List<Map<String, ?>> deaths = rawMessage.getMessageProperties().getXDeathHeader();
        if (deaths == null || deaths.isEmpty()) {
            return 0L;
        }
        return deaths.stream()
                .map(death -> death.get("count"))
                .filter(Number.class::isInstance)
                .map(Number.class::cast)
                .mapToLong(Number::longValue)
                .sum();
    }
}
