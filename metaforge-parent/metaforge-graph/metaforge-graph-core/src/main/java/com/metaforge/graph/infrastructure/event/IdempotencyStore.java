package com.metaforge.graph.infrastructure.event;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * 事件幂等存储——基于内存 ConcurrentHashMap 实现。
 * 以实体 FQN + 版本号为幂等键，避免重复事件导致的关系重建重复执行。
 */
@Component
public class IdempotencyStore {

    private final Map<String, String> processedKeys = new ConcurrentHashMap<>();

    /**
     * 检查事件是否已处理。
     *
     * @param entityFqn 实体 FQN
     * @param version   实体版本号
     * @return true 表示已处理过，应跳过
     */
    public boolean isProcessed(String entityFqn, Integer version) {
        String key = buildKey(entityFqn, version);
        return processedKeys.containsKey(key);
    }

    /**
     * 标记事件为已处理。
     *
     * @param entityFqn 实体 FQN
     * @param version   实体版本号
     */
    public void markProcessed(String entityFqn, Integer version) {
        String key = buildKey(entityFqn, version);
        processedKeys.put(key, key);
    }

    /**
     * 清除指定实体的幂等记录（供重试场景使用）。
     */
    public void clear(String entityFqn) {
        processedKeys.entrySet().removeIf(entry -> entry.getKey().startsWith(entityFqn + "#"));
    }

    private String buildKey(String entityFqn, Integer version) {
        return entityFqn + "#" + version;
    }
}
