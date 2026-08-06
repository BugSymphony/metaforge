package com.metaforge.server.config;

import java.util.concurrent.TimeUnit;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;

import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Caffeine 缓存配置
 * <p>配置基于 Caffeine 的本地缓存管理器，设置过期时间、最大容量和统计功能。</p>
 */
@AutoConfiguration
public class CacheConfig {

    /**
     * 创建 Caffeine 缓存管理器
     * <p>expireAfterWrite=30分钟，maximumSize=1000，开启命中统计。</p>
     *
     * @return CaffeineCacheManager 实例
     */
    @Bean
    public CaffeineCacheManager cacheManager() {
        Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .maximumSize(1000)
                .recordStats();
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(caffeine);
        return cacheManager;
    }
}
