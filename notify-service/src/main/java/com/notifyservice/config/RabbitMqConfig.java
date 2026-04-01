package com.notifyservice.config;

import com.blogcommon.message.MqConstants;
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
}
