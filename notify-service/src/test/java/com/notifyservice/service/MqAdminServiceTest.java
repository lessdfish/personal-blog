package com.notifyservice.service;

import com.blogcommon.message.MqConstants;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MqAdminServiceTest {
    private final RabbitAdmin rabbitAdmin = mock(RabbitAdmin.class);
    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final MqAdminService mqAdminService = new MqAdminService(rabbitAdmin, rabbitTemplate);

    @Test
    void getDlqOverviewShouldReadQueueCounts() {
        when(rabbitAdmin.getQueueProperties(MqConstants.COMMENT_NOTIFY_DLQ)).thenReturn(properties(2));
        when(rabbitAdmin.getQueueProperties(MqConstants.ARTICLE_INTERACTION_NOTIFY_DLQ)).thenReturn(properties(3));

        MqAdminService.DlqOverview overview = mqAdminService.getDlqOverview();

        assertEquals(2, overview.queues().size());
        assertEquals(5L, overview.queues().stream().mapToLong(MqAdminService.DlqStats::messages).sum());
    }

    @Test
    void requeueShouldStopWhenQueueIsEmpty() {
        Message first = MessageBuilder.withBody("one".getBytes()).build();
        Message second = MessageBuilder.withBody("two".getBytes()).build();
        when(rabbitTemplate.receive(MqConstants.COMMENT_NOTIFY_DLQ)).thenReturn(first, second, null);

        MqAdminService.RequeueResult result = mqAdminService.requeue("comment", 10);

        assertEquals(2, result.republishedCount());
        verify(rabbitTemplate, times(3)).receive(MqConstants.COMMENT_NOTIFY_DLQ);
        verify(rabbitTemplate).send(MqConstants.COMMENT_NOTIFY_EXCHANGE, MqConstants.COMMENT_NOTIFY_ROUTING_KEY, first);
        verify(rabbitTemplate).send(MqConstants.COMMENT_NOTIFY_EXCHANGE, MqConstants.COMMENT_NOTIFY_ROUTING_KEY, second);
    }

    private Properties properties(long messages) {
        Properties properties = new Properties();
        properties.put(RabbitAdmin.QUEUE_MESSAGE_COUNT, messages);
        return properties;
    }
}
