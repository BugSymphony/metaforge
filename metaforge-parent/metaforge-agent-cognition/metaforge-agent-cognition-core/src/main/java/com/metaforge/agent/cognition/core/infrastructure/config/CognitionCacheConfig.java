package com.metaforge.agent.cognition.core.infrastructure.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CognitionCacheConfig {

    @Bean("cognitionResultCache")
    public com.github.benmanes.caffeine.cache.Cache<String, Object> cognitionResultCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .maximumSize(100)
                .recordStats()
                .build();
    }

    @Bean("perspectiveResultCache")
    public com.github.benmanes.caffeine.cache.Cache<String, Object> perspectiveResultCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .maximumSize(500)
                .recordStats()
                .build();
    }
}
