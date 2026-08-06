package com.metaforge.server.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

import com.metaforge.common.spi.LogMaskSpi;

/**
 * 日志配置
 * <p>提供默认的日志脱敏规则，对敏感字段（password、secret、token、phone、email、idCard）
 * 进行自动脱敏处理，防止敏感信息泄露到日志中。</p>
 */
@AutoConfiguration
public class LogConfig {

    /**
     * 创建默认的日志脱敏 SPI 实现
     * <p>默认脱敏规则：</p>
     * <ul>
     *   <li>长度 ≤ 4：替换为 "****"</li>
     *   <li>长度 > 4：保留首尾各 2 个字符，中间替换为 "****"</li>
     * </ul>
     *
     * @return LogMaskSpi 实例
     */
    @Bean
    public LogMaskSpi defaultLogMaskSpi() {
        return (fieldName, fieldValue) -> {
            if (fieldValue == null) return null;
            if (isSensitiveField(fieldName)) {
                return maskSensitive(fieldValue);
            }
            return fieldValue;
        };
    }

    /**
     * 判断字段是否为敏感字段
     *
     * @param name 字段名称
     * @return true 表示敏感字段
     */
    private boolean isSensitiveField(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase();
        return lower.contains("password") || lower.contains("secret") || lower.contains("token")
            || lower.contains("phone") || lower.contains("email") || lower.contains("idcard");
    }

    /**
     * 对敏感值进行脱敏处理
     * <p>保留首尾各 2 个字符，中间用 "****" 替换。</p>
     *
     * @param value 原始值
     * @return 脱敏后的值
     */
    private String maskSensitive(String value) {
        if (value.length() <= 4) return "****";
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }
}
