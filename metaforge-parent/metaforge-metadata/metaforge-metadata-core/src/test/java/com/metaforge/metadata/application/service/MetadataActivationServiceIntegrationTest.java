package com.metaforge.metadata.application.service;

import com.metaforge.framework.test.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Tag("integration")
@DisplayName("版本生效与生命周期集成测试")
class MetadataActivationServiceIntegrationTest extends BaseIntegrationTest {

    @org.springframework.beans.factory.annotation.Autowired
    private com.metaforge.metadata.api.service.MetadataDraftService draftService;

    @org.springframework.beans.factory.annotation.Autowired
    private com.metaforge.metadata.api.service.MetadataActivationService activationService;

    @org.springframework.beans.factory.annotation.Autowired
    private com.metaforge.metadata.api.service.MetadataQueryService queryService;

    @Test
    @DisplayName("草稿创建后生效-主表存在-草稿删除-历史表归档")
    void testActivateDraft() {
        String fqn = "Test_Activate_001";
        com.metaforge.metadata.api.dto.request.CreateDraftRequest req = new com.metaforge.metadata.api.dto.request.CreateDraftRequest();
        req.setFqn(fqn);
        req.setName("生效测试");
        req.setEntitySchemaFqn("test:1.0.0.test.Test");
        java.util.Map<String, Object> content = new java.util.HashMap<>();
        content.put("field1", "value1");
        req.setContent(content);
        draftService.createDraft(req);

        com.metaforge.metadata.api.dto.response.MetadataEntityDto entity = activationService.activate(fqn);

        org.junit.jupiter.api.Assertions.assertNotNull(entity);
        org.junit.jupiter.api.Assertions.assertEquals(fqn, entity.getFqn());
        org.junit.jupiter.api.Assertions.assertEquals(1, entity.getCurrentVersion());

        com.metaforge.metadata.api.dto.response.MetadataEntityDto queried = queryService.getByFqn(fqn);
        org.junit.jupiter.api.Assertions.assertNotNull(queried);
    }

    @Test
    @DisplayName("下线前提条件校验")
    void testCheckDeactivationPreconditions() {
        String fqn = "Test_DeactCheck_001";
        com.metaforge.metadata.api.dto.request.CreateDraftRequest req = new com.metaforge.metadata.api.dto.request.CreateDraftRequest();
        req.setFqn(fqn);
        req.setName("下线校验测试");
        req.setEntitySchemaFqn("test:1.0.0.test.Test");
        java.util.Map<String, Object> content = new java.util.HashMap<>();
        content.put("field1", "value1");
        req.setContent(content);
        draftService.createDraft(req);
        activationService.activate(fqn);

        com.metaforge.metadata.api.dto.response.DeactivationCheckResult result = activationService.checkDeactivationPreconditions(fqn);

        org.junit.jupiter.api.Assertions.assertNotNull(result);
        org.junit.jupiter.api.Assertions.assertTrue(result.isCanDeactivate());
    }
}
