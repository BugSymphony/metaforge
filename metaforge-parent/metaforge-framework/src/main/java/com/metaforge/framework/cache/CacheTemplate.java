package com.metaforge.framework.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

/**
 * 缓存操作模板，统一 Key 格式为 bcName:entity:id。
 */
@Component
public class CacheTemplate {

    private final CacheManager cacheManager;

    /**
     * 构造函数注入 CacheManager。
     *
     * @param cacheManager Spring CacheManager（Caffeine 实现）
     */
    public CacheTemplate(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * 存入缓存。Key 格式：bcName:entity:id
     *
     * @param bcName 业务域名称（对应 Cache 名称）
     * @param entity 实体类型
     * @param id     实体 ID
     * @param value  缓存值
     */
    public void put(String bcName, String entity, String id, Object value) {
        String cacheKey = bcName + ":" + entity + ":" + id;
        Cache cache = cacheManager.getCache(bcName);
        if (cache != null) {
            cache.put(cacheKey, value);
        }
    }

    /**
     * 读取缓存。
     *
     * @param bcName 业务域名称
     * @param entity 实体类型
     * @param id     实体 ID
     * @param <T>    缓存值类型
     * @return 缓存值；未命中时返回 null
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String bcName, String entity, String id) {
        String cacheKey = bcName + ":" + entity + ":" + id;
        Cache cache = cacheManager.getCache(bcName);
        if (cache != null) {
            Cache.ValueWrapper wrapper = cache.get(cacheKey);
            if (wrapper != null) {
                return (T) wrapper.get();
            }
        }
        return null;
    }

    /**
     * 清除缓存。
     *
     * @param bcName 业务域名称
     * @param entity 实体类型
     * @param id     实体 ID
     */
    public void evict(String bcName, String entity, String id) {
        String cacheKey = bcName + ":" + entity + ":" + id;
        Cache cache = cacheManager.getCache(bcName);
        if (cache != null) {
            cache.evict(cacheKey);
        }
    }
}
