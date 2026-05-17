package com.commentservice.config;

import com.blogcommon.message.MqConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;

import javax.management.Query;

    /**
     * 配置 commentNotifyExchange：为当前服务准备运行时需要的组件或参数。
     */
/**
 * ClassName:Rabbitmq
 * Package:com.commentservice.config
 * Description:
 *
 * @Author:lyp
 * @Create:2026/3/31 - 23:32
 * @Version: v1.0
 *
 */
public class Rabbitmq {
    @Bean
    public DirectExchange commentNotifyExchange(){
        return new DirectExchange(MqConstants.COMMENT_NOTIFY_EXCHANGE,true,false);
    }

    /**
     * 配置 commentNotifyQueue：为当前服务准备运行时需要的组件或参数。
     */
    @Bean
    public Queue commentNotifyQueue(){
        return new Queue(MqConstants.COMMENT_NOTIFY_QUEUE,true);
    }

    /**
     * 配置 commentNotifyBinding：为当前服务准备运行时需要的组件或参数。
     */
    @Bean
    public Binding commentNotifyBinding(Queue commentNotifyQueue,DirectExchange commentNotifyExchange){
        return BindingBuilder.bind(commentNotifyQueue)
                .to(commentNotifyExchange)
                .with(MqConstants.COMMENT_NOTIFY_ROUTING_KEY);
    }
}
