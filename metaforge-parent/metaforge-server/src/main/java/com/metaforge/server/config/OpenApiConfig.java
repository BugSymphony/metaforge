package com.metaforge.server.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/**
 * SpringDoc OpenAPI 配置
 * <p>配置 OpenAPI 文档基本信息及分组策略，为 MetaForge 平台生成 API 文档。</p>
 */
@AutoConfiguration
public class OpenApiConfig {

    /**
     * 创建 OpenAPI 实例
     *
     * @return OpenAPI 实例，包含 MetaForge 平台 API 文档标题、版本和描述
     */
    @Bean
    public OpenAPI metaForgeOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MetaForge API")
                        .version("1.0.0")
                        .description("MetaForge 平台 API 文档"));
    }

    /**
     * 创建 "all" 分组 API
     * <p>匹配所有路径 /**，将所有接口归入同一分组。</p>
     *
     * @return GroupedOpenApi 实例
     */
    @Bean
    public GroupedOpenApi allGroupedOpenApi() {
        return GroupedOpenApi.builder()
                .group("all")
                .pathsToMatch("/**")
                .build();
    }
}
