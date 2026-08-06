package com.metaforge.metadata.infrastructure.spi;

import com.metaforge.common.spi.HealthCheckSpi;
import org.springframework.stereotype.Component;

@Component
public class MetadataHealthCheck implements HealthCheckSpi {

    @Override
    public HealthCheckResult check() {
        return new HealthCheckResult(
                "metadata-management",
                true,
                "元数据管理 BC 运行正常"
        );
    }
}
