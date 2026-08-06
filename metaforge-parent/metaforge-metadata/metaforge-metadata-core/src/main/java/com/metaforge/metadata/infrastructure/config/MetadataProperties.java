package com.metaforge.metadata.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "metaforge.metadata")
public class MetadataProperties {

    /** JSON Schema 校验缓存 TTL，默认 30 分钟 */
    private Duration schemaValidationCacheTtl = Duration.ofMinutes(30);

    /** 批量导入单批次最大条数，默认 500 */
    private int importMaxBatchSize = 500;

    /** 导出默认格式，默认 json */
    private String exportDefaultFormat = "json";

    /** 历史表只读保护开关，默认 true */
    private boolean historyReadonly = true;

    public Duration getSchemaValidationCacheTtl() { return schemaValidationCacheTtl; }
    public void setSchemaValidationCacheTtl(Duration schemaValidationCacheTtl) { this.schemaValidationCacheTtl = schemaValidationCacheTtl; }

    public int getImportMaxBatchSize() { return importMaxBatchSize; }
    public void setImportMaxBatchSize(int importMaxBatchSize) { this.importMaxBatchSize = importMaxBatchSize; }

    public String getExportDefaultFormat() { return exportDefaultFormat; }
    public void setExportDefaultFormat(String exportDefaultFormat) { this.exportDefaultFormat = exportDefaultFormat; }

    public boolean isHistoryReadonly() { return historyReadonly; }
    public void setHistoryReadonly(boolean historyReadonly) { this.historyReadonly = historyReadonly; }
}
