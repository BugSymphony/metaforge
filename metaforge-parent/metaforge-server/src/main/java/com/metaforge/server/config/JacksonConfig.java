package com.metaforge.server.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Jackson 序列化配置
 * <p>统一配置 JSON 序列化行为，包括日期格式、时区、空值处理等。</p>
 */
@AutoConfiguration(after = VirtualThreadConfig.class)
public class JacksonConfig {

    /**
     * 自定义 Jackson ObjectMapper 构建器
     *
     * @return Jackson2ObjectMapperBuilderCustomizer 实例
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            // 设置时区为 Asia/Shanghai
            builder.timeZone("Asia/Shanghai");
            // 序列化时排除 null 字段
            builder.serializationInclusion(JsonInclude.Include.NON_NULL);
            // 禁用时间戳格式（使用字符串格式）
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            // 注册 Java 8 时间模块
            builder.modules(new JavaTimeModule());
        };
    }
}
