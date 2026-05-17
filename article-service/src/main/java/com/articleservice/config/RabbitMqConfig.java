package com.articleservice.config;

import com.blogcommon.message.MqConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class RabbitMqConfig {

    /**
     * 创建消息转换器：把 Java 对象和 RabbitMQ 消息之间用 JSON 互相转换。
     */
    @Bean
    public MessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 创建 RabbitTemplate：用于发送 RabbitMQ 消息，并记录发送失败或路由失败的日志。
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter rabbitMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(rabbitMessageConverter);
        rabbitTemplate.setMandatory(true);
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.error("RabbitMQ消息确认失败, correlationData={}, cause={}", correlationData, cause);
            }
        });
        rabbitTemplate.setReturnsCallback(returned -> log.error(
                "RabbitMQ消息路由失败, exchange={}, routingKey={}, replyCode={}, replyText={}",
                returned.getExchange(),
                returned.getRoutingKey(),
                returned.getReplyCode(),
                returned.getReplyText()));
        return rabbitTemplate;
    }

    /**
     * 创建文章搜索同步交换机：文章变更后把同步消息发到这里。
     */
    @Bean
    public DirectExchange articleEsSyncExchange() {
        return ExchangeBuilder.directExchange(MqConstants.ARTICLE_ES_SYNC_EXCHANGE).build();
    }

    /**
     * 创建文章搜索同步队列：消费者从这个队列里取消息来更新 ES 索引。
     */
    @Bean
    public Queue articleEsSyncQueue() {
        return QueueBuilder.durable(MqConstants.ARTICLE_ES_SYNC_QUEUE).build();
    }

    /**
     * 绑定交换机和队列：规定文章搜索同步消息应该进入哪个队列。
     */
    @Bean
    public Binding articleEsSyncBinding() {
        return BindingBuilder.bind(articleEsSyncQueue())
                .to(articleEsSyncExchange())
                .with(MqConstants.ARTICLE_ES_SYNC_ROUTING_KEY);
    }
}
