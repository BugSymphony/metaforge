package com.metaforge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * MetaForge 应用启动主类，全平台唯一运行入口。
 *
 * <p>通过 @ComponentScan("com.metaforge") 统一扫描根包，
 * 确保平台模块与所有注册的业务 BC 的 Bean 被自动装载。
 */
@SpringBootApplication
@ComponentScan("com.metaforge")
public class MetaforgeApplication {
    public static void main(String[] args) {
        SpringApplication.run(MetaforgeApplication.class, args);
    }
}
