package com.metaforge.metamodel.infrastructure.spi;

import com.metaforge.common.spi.HealthCheckSpi;

import org.springframework.stereotype.Component;

/**
 * 元模型治理 BC 健康检查 SPI 实现。
 */
@Component
public class MetamodelHealthCheck implements HealthCheckSpi {

    @Override
    public HealthCheckResult check() {
        return new HealthCheckResult(
                "metamodel-governance",
                true,
                "元模型治理 BC 运行正常"
        );
    }
}
