package com.metaforge.graph.contract_adapt;

import com.metaforge.framework.test.BaseIntegrationTest;
import com.metaforge.graph.infrastructure.persistence.adapter.GraphMetadataGatewayAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 上游对接测试——验证 GraphMetadataGatewayAdapter 与 metaforge-metadata-api 对接正确。
 */
@SpringBootTest
@Tag("integration")
@DisplayName("Metadata 网关契约测试")
class MetadataGatewayContractTest extends BaseIntegrationTest {

    @Autowired
    private GraphMetadataGatewayAdapter gatewayAdapter;

    @Test
    @DisplayName("isEntityActive 返回 true（MVP 默认）")
    void testIsEntityActive() {
        assertTrue(gatewayAdapter.isEntityActive("test:entity:1"));
    }

    @Test
    @DisplayName("getEntityInfo 返回非空")
    void testGetEntityInfo() {
        String result = gatewayAdapter.getEntityInfo("test:entity:1");
        assertNotNull(result);
        assertEquals("test:entity:1", result);
    }
}
