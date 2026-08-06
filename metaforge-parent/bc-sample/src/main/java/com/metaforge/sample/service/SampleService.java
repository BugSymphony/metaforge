package com.metaforge.sample.service;

import com.metaforge.sample.model.SampleEntity;
import com.metaforge.sample.repository.SampleRepository;
import com.metaforge.framework.cache.CacheTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 示例 Service，演示 CacheManager 注入、缓存读写与自定义异常。
 */
@Service
public class SampleService {
    private static final Logger log = LoggerFactory.getLogger(SampleService.class);
    private final SampleRepository sampleRepository;
    private final CacheManager cacheManager;
    private final CacheTemplate cacheTemplate;

    public SampleService(SampleRepository sampleRepository, CacheManager cacheManager, CacheTemplate cacheTemplate) {
        this.sampleRepository = sampleRepository;
        this.cacheManager = cacheManager;
        this.cacheTemplate = cacheTemplate;
    }

    public List<SampleEntity> findAll() {
        return sampleRepository.findAll();
    }

    public String getCacheValue(String key) {
        return cacheTemplate.get("bc-sample", "cache", key);
    }

    public void setCacheValue(String key, String value) {
        cacheTemplate.put("bc-sample", "cache", key, value);
    }

    public String hello() {
        log.info("处理 hello 请求");
        return "Hello from MetaForge bc-sample!";
    }
}
