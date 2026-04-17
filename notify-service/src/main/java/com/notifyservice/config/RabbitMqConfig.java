package com.notifyservice.config;

import com.blogcommon.message.MqConstants;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.core.*;
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

    @Bean
    public MessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter rabbitMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(rabbitMessageConverter);
        return rabbitTemplate;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory,
                                                                              MessageConverter rabbitMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(rabbitMessageConverter);
        return factory;
    }

    @Bean
    public DirectExchange commentNotifyExchange() {
        return ExchangeBuilder.directExchange(MqConstants.COMMENT_NOTIFY_EXCHANGE).build();
    }

    @Bean
    public Queue commentNotifyQueue() {
        return QueueBuilder.durable(MqConstants.COMMENT_NOTIFY_QUEUE).build();
    }

    @Bean
    public Binding commentNotifyBinding() {
        return BindingBuilder.bind(commentNotifyQueue())
                .to(commentNotifyExchange())
                .with(MqConstants.COMMENT_NOTIFY_ROUTING_KEY);
    }

    @Bean
    public DirectExchange articleInteractionNotifyExchange() {
        return ExchangeBuilder.directExchange(MqConstants.ARTICLE_INTERACTION_NOTIFY_EXCHANGE).build();
    }

    @Bean
    public Queue articleInteractionNotifyQueue() {
        return QueueBuilder.durable(MqConstants.ARTICLE_INTERACTION_NOTIFY_QUEUE).build();
    }

    @Bean
    public Binding articleInteractionNotifyBinding() {
        return BindingBuilder.bind(articleInteractionNotifyQueue())
                .to(articleInteractionNotifyExchange())
                .with(MqConstants.ARTICLE_INTERACTION_NOTIFY_ROUTING_KEY);
    }
}
