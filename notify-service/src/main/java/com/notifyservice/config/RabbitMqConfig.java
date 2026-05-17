package com.notifyservice.config;

import com.blogcommon.message.MqConstants;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ClassName:RabbitMqConfig
 * Package:com.notifyservice.config
 * Description:RabbitMQ配置
 *
 * @Author:lyp
 * @Create:2026/4/1
 * @Version: v1.0
 */
@Configuration
public class RabbitMqConfig {
    private static final int RETRY_DELAY_MILLISECONDS = 5000;

    /**
     * 创建消息转换器：让 RabbitMQ 消息和 Java 对象之间用 JSON 互相转换。
     */
    @Bean
    public MessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 创建 RabbitTemplate：用于向 RabbitMQ 发送消息。
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter rabbitMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(rabbitMessageConverter);
        return rabbitTemplate;
    }

    /**
     * 创建 RabbitAdmin：用于查询或管理 RabbitMQ 队列信息。
     */
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    /**
     * 创建 RabbitMQ 监听容器工厂：配置消费者的手动确认方式。
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory,
                                                                              MessageConverter rabbitMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(rabbitMessageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        return factory;
    }

    /**
     * 配置 commentNotifyExchange：为当前服务准备运行时需要的组件或参数。
     */
    @Bean
    public DirectExchange commentNotifyExchange() {
        return ExchangeBuilder.directExchange(MqConstants.COMMENT_NOTIFY_EXCHANGE).build();
    }

    /**
     * 配置 commentNotifyQueue：为当前服务准备运行时需要的组件或参数。
     */
    @Bean
    public Queue commentNotifyQueue() {
        return QueueBuilder.durable(MqConstants.COMMENT_NOTIFY_QUEUE)
                .deadLetterExchange(MqConstants.COMMENT_NOTIFY_DLX)
                .deadLetterRoutingKey(MqConstants.COMMENT_NOTIFY_RETRY_ROUTING_KEY)
                .build();
    }

    /**
     * 配置 commentNotifyBinding：为当前服务准备运行时需要的组件或参数。
     */
    @Bean
    public Binding commentNotifyBinding() {
        return BindingBuilder.bind(commentNotifyQueue())
                .to(commentNotifyExchange())
                .with(MqConstants.COMMENT_NOTIFY_ROUTING_KEY);
    }

    /**
     * 配置 commentNotifyDlx：为当前服务准备运行时需要的组件或参数。
     */
    @Bean
    public DirectExchange commentNotifyDlx() {
        return ExchangeBuilder.directExchange(MqConstants.COMMENT_NOTIFY_DLX).build();
    }

    /**
     * 配置 commentNotifyRetryQueue：为当前服务准备运行时需要的组件或参数。
     */
    @Bean
    public Queue commentNotifyRetryQueue() {
        return QueueBuilder.durable(MqConstants.COMMENT_NOTIFY_RETRY_QUEUE)
                .ttl(RETRY_DELAY_MILLISECONDS)
                .deadLetterExchange(MqConstants.COMMENT_NOTIFY_EXCHANGE)
                .deadLetterRoutingKey(MqConstants.COMMENT_NOTIFY_ROUTING_KEY)
                .build();
    }

    /**
     * 配置 commentNotifyRetryBinding：为当前服务准备运行时需要的组件或参数。
     */
    @Bean
    public Binding commentNotifyRetryBinding() {
        return BindingBuilder.bind(commentNotifyRetryQueue())
                .to(commentNotifyDlx())
                .with(MqConstants.COMMENT_NOTIFY_RETRY_ROUTING_KEY);
    }

    /**
     * 配置 commentNotifyDlq：为当前服务准备运行时需要的组件或参数。
     */
    @Bean
    public Queue commentNotifyDlq() {
        return QueueBuilder.durable(MqConstants.COMMENT_NOTIFY_DLQ).build();
    }

    /**
     * 配置 commentNotifyDlqBinding：为当前服务准备运行时需要的组件或参数。
     */
    @Bean
    public Binding commentNotifyDlqBinding() {
        return BindingBuilder.bind(commentNotifyDlq())
                .to(commentNotifyDlx())
                .with(MqConstants.COMMENT_NOTIFY_DLQ_ROUTING_KEY);
    }

    /**
     * 配置 articleInteractionNotifyExchange：为当前服务准备运行时需要的组件或参数。
     */
    @Bean
    public DirectExchange articleInteractionNotifyExchange() {
        return ExchangeBuilder.directExchange(MqConstants.ARTICLE_INTERACTION_NOTIFY_EXCHANGE).build();
    }

    /**
     * 配置 articleInteractionNotifyQueue：为当前服务准备运行时需要的组件或参数。
     */
    @Bean
    public Queue articleInteractionNotifyQueue() {
        return QueueBuilder.durable(MqConstants.ARTICLE_INTERACTION_NOTIFY_QUEUE)
                .deadLetterExchange(MqConstants.ARTICLE_INTERACTION_NOTIFY_DLX)
                .deadLetterRoutingKey(MqConstants.ARTICLE_INTERACTION_NOTIFY_RETRY_ROUTING_KEY)
                .build();
    }

    /**
     * 配置 articleInteractionNotifyBinding：为当前服务准备运行时需要的组件或参数。
     */
    @Bean
    public Binding articleInteractionNotifyBinding() {
        return BindingBuilder.bind(articleInteractionNotifyQueue())
                .to(articleInteractionNotifyExchange())
                .with(MqConstants.ARTICLE_INTERACTION_NOTIFY_ROUTING_KEY);
    }

    /**
     * 配置 articleInteractionNotifyDlx：为当前服务准备运行时需要的组件或参数。
     */
    @Bean
    public DirectExchange articleInteractionNotifyDlx() {
        return ExchangeBuilder.directExchange(MqConstants.ARTICLE_INTERACTION_NOTIFY_DLX).build();
    }

    /**
     * 配置 articleInteractionNotifyRetryQueue：为当前服务准备运行时需要的组件或参数。
     */
    @Bean
    public Queue articleInteractionNotifyRetryQueue() {
        return QueueBuilder.durable(MqConstants.ARTICLE_INTERACTION_NOTIFY_RETRY_QUEUE)
                .ttl(RETRY_DELAY_MILLISECONDS)
                .deadLetterExchange(MqConstants.ARTICLE_INTERACTION_NOTIFY_EXCHANGE)
                .deadLetterRoutingKey(MqConstants.ARTICLE_INTERACTION_NOTIFY_ROUTING_KEY)
                .build();
    }

    /**
     * 配置 articleInteractionNotifyRetryBinding：为当前服务准备运行时需要的组件或参数。
     */
    @Bean
    public Binding articleInteractionNotifyRetryBinding() {
        return BindingBuilder.bind(articleInteractionNotifyRetryQueue())
                .to(articleInteractionNotifyDlx())
                .with(MqConstants.ARTICLE_INTERACTION_NOTIFY_RETRY_ROUTING_KEY);
    }

    /**
     * 配置 articleInteractionNotifyDlq：为当前服务准备运行时需要的组件或参数。
     */
    @Bean
    public Queue articleInteractionNotifyDlq() {
        return QueueBuilder.durable(MqConstants.ARTICLE_INTERACTION_NOTIFY_DLQ).build();
    }

    /**
     * 配置 articleInteractionNotifyDlqBinding：为当前服务准备运行时需要的组件或参数。
     */
    @Bean
    public Binding articleInteractionNotifyDlqBinding() {
        return BindingBuilder.bind(articleInteractionNotifyDlq())
                .to(articleInteractionNotifyDlx())
                .with(MqConstants.ARTICLE_INTERACTION_NOTIFY_DLQ_ROUTING_KEY);
    }
}
