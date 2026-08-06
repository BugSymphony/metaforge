package com.metaforge.server.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Schema 守卫工具配置
 * <p>提供 Schema 写入权限校验工具，基于 BC 名称与 Schema 名称白名单机制控制写入权限。</p>
 */
@AutoConfiguration
public class SchemaGuardConfig {

    /**
     * 创建 SchemaGuard 实例
     *
     * @return SchemaGuard 实例
     */
    @Bean
    public SchemaGuard schemaGuard() {
        return new SchemaGuard();
    }

    /**
     * Schema 守卫工具类
     * <p>维护 BC-Schema 白名单映射，提供写入权限校验。</p>
     */
    public static class SchemaGuard {

        /**
         * BC 名称到 Schema 名称集合的白名单映射
         */
        private final Map<String, Set<String>> bcSchemaWhitelist;

        /**
         * 构造 SchemaGuard，初始化白名单
         */
        public SchemaGuard() {
            this.bcSchemaWhitelist = new HashMap<>();
        }

        /**
         * 注册 BC 下允许写入的 Schema 白名单
         *
         * @param bcName     BC 名称
         * @param schemas    允许写入的 Schema 名称集合
         */
        public void registerWhitelist(String bcName, Set<String> schemas) {
            bcSchemaWhitelist.put(bcName, new HashSet<>(schemas));
        }

        /**
         * 获取 BC-Schema 白名单映射（只读视图）
         *
         * @return 白名单映射
         */
        public Map<String, Set<String>> getBcSchemaWhitelist() {
            return Collections.unmodifiableMap(bcSchemaWhitelist);
        }

        /**
         * 检查指定 BC 和 Schema 是否具有写入权限
         *
         * @param bcName     BC 名称
         * @param schemaName Schema 名称
         * @return true 表示允许写入，false 表示无写入权限
         */
        public boolean checkWritePermission(String bcName, String schemaName) {
            if (bcName == null || schemaName == null) {
                return false;
            }
            Set<String> schemas = bcSchemaWhitelist.get(bcName);
            if (schemas == null) {
                return false;
            }
            return schemas.contains(schemaName);
        }
    }
}
