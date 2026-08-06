package com.metaforge.server.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 数据源与事务管理配置
 * <p>启用 Spring 事务管理，数据源相关属性由 application.yml 自动读取并配置。</p>
 */
@AutoConfiguration
@EnableTransactionManagement
public class DataSourceConfig {

}
