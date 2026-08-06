package com.metaforge.computeengine.infrastructure.config;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * jOOQ DSLContext Bean 配置。
 *
 * <p>注入 foundation-core 统一管理的 DataSource，构建 jOOQ DSLContext 实例。
 * 使用 PostgreSQL 方言。
 *
 * @author metaforge
 */
@Configuration
public class JooqConfig {

    @Bean
    public DSLContext dslContext(DataSource dataSource) {
        return DSL.using(dataSource, SQLDialect.POSTGRES);
    }
}
