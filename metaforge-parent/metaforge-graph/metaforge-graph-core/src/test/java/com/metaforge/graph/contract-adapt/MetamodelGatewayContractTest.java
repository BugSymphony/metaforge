package com.metaforge.graph.contract_adapt;

import com.metaforge.framework.test.BaseIntegrationTest;
import com.metaforge.graph.infrastructure.persistence.adapter.GraphMetamodelGatewayAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 上游对接测试——验证 GraphMetamodelGatewayAdapter 与 metaforge-metamodel-api 对接正确。
 */
@SpringBootTest
@Tag("integration")
@DisplayName("Metamodel 网关契约测试")
class MetamodelGatewayContractTest extends BaseIntegrationTest {

    @Autowired
    private GraphMetamodelGatewayAdapter gatewayAdapter;

    @Test
    @DisplayName("getRelationSchemaSchema 返回非空")
    void testGetRelationSchemaSchema() {
        String result = gatewayAdapter.getRelationSchemaSchema("test:schema:1.0.0");
        assertNotNull(result);
    }

    @Test
    @DisplayName("isSchemaPublished 返回 true（MVP 默认）")
    void testIsSchemaPublished() {
        assertTrue(gatewayAdapter.isSchemaPublished("test:schema:1.0.0"));
    }
}
