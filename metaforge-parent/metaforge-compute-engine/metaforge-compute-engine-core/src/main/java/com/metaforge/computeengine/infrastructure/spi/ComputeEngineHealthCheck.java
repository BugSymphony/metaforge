package com.metaforge.computeengine.infrastructure.spi;

import com.metaforge.common.spi.HealthCheckSpi;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static org.jooq.impl.DSL.*;

/**
 * 语义查询与推理引擎 BC 健康检查 SPI 实现。
 *
 * <p>检查上游 BC 数据表（metadata_management.metadata_entity、semantic_relation_network.relation_instance）的可读性。
 *
 * @author metaforge
 */
@Component
public class ComputeEngineHealthCheck implements HealthCheckSpi {

    private static final Logger log = LoggerFactory.getLogger(ComputeEngineHealthCheck.class);

    private final DSLContext dsl;

    public ComputeEngineHealthCheck(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public HealthCheckResult check() {
        boolean healthy = true;
        StringBuilder detail = new StringBuilder();

        try {
            dsl.selectCount()
                    .from(table(name("metadata_management", "metadata_entity")))
                    .fetchOne();
            detail.append("metadata_management.metadata_entity: 可读; ");
        } catch (Exception e) {
            healthy = false;
            detail.append("metadata_management.metadata_entity: 不可读(").append(e.getMessage()).append("); ");
            log.warn("metadata_management.metadata_entity 表健康检查失败", e);
        }

        try {
            dsl.selectCount()
                    .from(table(name("semantic_relation_network", "relation_instance")))
                    .fetchOne();
            detail.append("semantic_relation_network.relation_instance: 可读; ");
        } catch (Exception e) {
            healthy = false;
            detail.append("semantic_relation_network.relation_instance: 不可读(").append(e.getMessage()).append("); ");
            log.warn("semantic_relation_network.relation_instance 表健康检查失败", e);
        }

        return new HealthCheckResult(
                "compute-engine-upstream",
                healthy,
                detail.toString().trim()
        );
    }
}
