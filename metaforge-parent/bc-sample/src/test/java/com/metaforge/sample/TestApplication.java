package com.metaforge.sample;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * 测试专用的 Spring Boot 应用配置。
 * 用于集成测试中的 Spring 上下文引导，不依赖 metaforge-boot 启动模块。
 */
@SpringBootApplication
@ComponentScan("com.metaforge")
public class TestApplication {
}
