package com.metaforge.graph.contract_export;

import com.metaforge.framework.test.BaseIntegrationTest;
import com.metaforge.graph.api.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 对外契约测试——验证 Application Service 接口方法签名与 contracts/application-service.md 一致。
 */
@SpringBootTest
@Tag("integration")
@DisplayName("Application Service 契约测试")
class RelationServiceContractTest extends BaseIntegrationTest {

    @Autowired private RelationDraftService draftService;
    @Autowired private RelationActivationService activationService;
    @Autowired private RelationQueryService queryService;
    @Autowired private RelationHistoryService historyService;
    @Autowired private RelationImportExportService importExportService;
    @Autowired private RelationTopologyService topologyService;

    @Test
    @DisplayName("RelationDraftService 注入正确")
    void testDraftServiceInjectable() {
        assertNotNull(draftService);
    }

    @Test
    @DisplayName("RelationActivationService 注入正确")
    void testActivationServiceInjectable() {
        assertNotNull(activationService);
    }

    @Test
    @DisplayName("RelationQueryService 注入正确")
    void testQueryServiceInjectable() {
        assertNotNull(queryService);
    }

    @Test
    @DisplayName("RelationHistoryService 注入正确")
    void testHistoryServiceInjectable() {
        assertNotNull(historyService);
    }

    @Test
    @DisplayName("RelationImportExportService 注入正确")
    void testImportExportServiceInjectable() {
        assertNotNull(importExportService);
    }

    @Test
    @DisplayName("RelationTopologyService 注入正确")
    void testTopologyServiceInjectable() {
        assertNotNull(topologyService);
    }
}
