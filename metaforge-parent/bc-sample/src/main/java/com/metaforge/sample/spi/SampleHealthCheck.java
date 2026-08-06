package com.metaforge.sample.spi;

import com.metaforge.common.spi.HealthCheckSpi;
import org.springframework.stereotype.Component;

/**
 * bc-sample 健康检查，报告自身状态为健康。
 */
@Component
public class SampleHealthCheck implements HealthCheckSpi {
    @Override
    public HealthCheckResult check() {
        return new HealthCheckResult("bc-sample", true, "bc-sample 正常运行");
    }
}
