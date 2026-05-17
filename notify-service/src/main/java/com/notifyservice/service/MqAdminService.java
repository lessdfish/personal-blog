package com.notifyservice.service;

import com.blogcommon.message.MqConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

@Service
public class MqAdminService {
    private static final Logger log = LoggerFactory.getLogger(MqAdminService.class);
    private static final int DEFAULT_REQUEUE_LIMIT = 10;
    private static final int MAX_REQUEUE_LIMIT = 100;
    private static final Map<String, DlqTarget> DLQ_TARGETS = Map.of(
            "comment", new DlqTarget("comment", MqConstants.COMMENT_NOTIFY_DLQ, MqConstants.COMMENT_NOTIFY_EXCHANGE, MqConstants.COMMENT_NOTIFY_ROUTING_KEY),
            "articleInteraction", new DlqTarget("articleInteraction", MqConstants.ARTICLE_INTERACTION_NOTIFY_DLQ, MqConstants.ARTICLE_INTERACTION_NOTIFY_EXCHANGE, MqConstants.ARTICLE_INTERACTION_NOTIFY_ROUTING_KEY)
    );

    private final RabbitAdmin rabbitAdmin;
    private final RabbitTemplate rabbitTemplate;

    /**
     * 构造 MqAdminService：注入这个类运行时需要的依赖。
     */
    public MqAdminService(RabbitAdmin rabbitAdmin, RabbitTemplate rabbitTemplate) {
        this.rabbitAdmin = rabbitAdmin;
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * 查询死信队列概览：返回各死信队列当前堆积消息数。
     */
    public DlqOverview getDlqOverview() {
        List<DlqStats> stats = DLQ_TARGETS.values().stream()
                .map(target -> new DlqStats(target.name(), target.queue(), getMessageCount(target.queue())))
                .toList();
        return new DlqOverview(stats);
    }

    /**
     * 重新投递死信消息：把死信队列里的消息重新发送回业务交换机。
     */
    public RequeueResult requeue(String name, Integer count) {
        DlqTarget target = resolveTarget(name);
        int limit = count == null || count < 1 ? DEFAULT_REQUEUE_LIMIT : Math.min(count, MAX_REQUEUE_LIMIT);
        int republished = 0;
        for (int i = 0; i < limit; i++) {
            Message message = rabbitTemplate.receive(target.queue());
            if (message == null) {
                break;
            }
            rabbitTemplate.send(target.exchange(), target.routingKey(), message);
            republished++;
        }
        log.info("DLQ messages requeued, name={}, queue={}, requested={}, republished={}",
                target.name(), target.queue(), limit, republished);
        return new RequeueResult(target.name(), target.queue(), limit, republished);
    }

    /**
     * 解析死信队列目标：根据名称找到对应队列、交换机和路由键。
     */
    private DlqTarget resolveTarget(String name) {
        if (name == null) {
            throw new IllegalArgumentException("DLQ name is required");
        }
        DlqTarget target = DLQ_TARGETS.get(name);
        if (target == null) {
            target = DLQ_TARGETS.get(name.toLowerCase(Locale.ROOT));
        }
        if (target == null) {
            throw new IllegalArgumentException("Unsupported DLQ name: " + name);
        }
        return target;
    }

    /**
     * 查询队列消息数：从 RabbitMQ 队列属性中读取 messages 数量。
     */
    private long getMessageCount(String queue) {
        Properties properties = rabbitAdmin.getQueueProperties(queue);
        if (properties == null) {
            return 0L;
        }
        Object value = properties.get(RabbitAdmin.QUEUE_MESSAGE_COUNT);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            return Long.parseLong(text);
        }
        return 0L;
    }

    /**
     * 业务方法 DlqTarget：封装 MqAdminService 中对应的核心处理流程。
     */
    private record DlqTarget(String name, String queue, String exchange, String routingKey) {
    }

    /**
     * 业务方法 DlqStats：封装 MqAdminService 中对应的核心处理流程。
     */
    public record DlqStats(String name, String queue, long messages) {
    }

    /**
     * 业务方法 DlqOverview：封装 MqAdminService 中对应的核心处理流程。
     */
    public record DlqOverview(List<DlqStats> queues) {
    }

    /**
     * 业务方法 RequeueResult：封装 MqAdminService 中对应的核心处理流程。
     */
    public record RequeueResult(String name, String queue, int requestedCount, int republishedCount) {
    }
}
