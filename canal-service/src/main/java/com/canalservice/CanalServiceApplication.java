package com.canalservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Canal 监听服务启动类：独立消费 MySQL binlog 变更，不参与业务接口流量。
 */
@EnableScheduling
@EnableDiscoveryClient
@ConfigurationPropertiesScan
@SpringBootApplication(scanBasePackages = {"com.canalservice", "com.blogcommon"})
public class CanalServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CanalServiceApplication.class, args);
    }
}
