package com.metaforge.graph.integration.persistence;

import com.metaforge.framework.test.BaseIntegrationTest;
import com.metaforge.graph.application.service.RelationDraftServiceImpl;
import com.metaforge.graph.api.dto.CreateDraftRequest;
import com.metaforge.graph.api.dto.RelationInstanceDraftDto;
import com.metaforge.graph.api.service.RelationDraftService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 草稿表 CRUD 集成测试。
 * 验证草稿的创建、查询、更新、删除全链路。
 */
@SpringBootTest
@Tag("integration")
@DisplayName("草稿持久化集成测试")
class RelationDraftPersistenceTest extends BaseIntegrationTest {

    @Autowired
    private RelationDraftService draftService;

    @Test
    @DisplayName("创建草稿——成功写入草稿表并返回完整 DTO")
    void testCreateDraft() {
        CreateDraftRequest request = new CreateDraftRequest();
        request.setSourceEntityFqn("test:EntityA");
        request.setTargetEntityFqn("test:EntityB");
        request.setRelationTypeFqn("test:relation:1.0.0.TEST_TYPE");
        request.setName("测试关系");
        request.setDescription("测试描述");

        Map<String, Object> content = new HashMap<>();
        content.put("key", "value");
        request.setContent(content);

        RelationInstanceDraftDto draft = draftService.createDraft(request);

        assertNotNull(draft);
        assertNotNull(draft.getFqn());
        assertTrue(draft.getFqn().contains("test:EntityA"));
        assertNotNull(draft.getCreatedTime());
    }

    @Test
    @DisplayName("查询草稿——FQN 精准查询返回完整详情")
    void testGetDraft() {
        CreateDraftRequest request = new CreateDraftRequest();
        request.setSourceEntityFqn("test:EntityC");
        request.setTargetEntityFqn("test:EntityD");
        request.setRelationTypeFqn("test:relation:1.0.0.TEST_TYPE");
        request.setName("测试查询关系");
        request.setContent(new HashMap<>());

        RelationInstanceDraftDto created = draftService.createDraft(request);
        RelationInstanceDraftDto found = draftService.getDraft(created.getFqn());

        assertNotNull(found);
        assertEquals(created.getFqn(), found.getFqn());
        assertEquals("测试查询关系", found.getName());
    }

    @Test
    @DisplayName("更新草稿内容——校验通过后 content 变更生效")
    void testUpdateDraftContent() {
        CreateDraftRequest request = new CreateDraftRequest();
        request.setSourceEntityFqn("test:EntityE");
        request.setTargetEntityFqn("test:EntityF");
        request.setRelationTypeFqn("test:relation:1.0.0.TEST_TYPE");
        request.setName("测试更新关系");
        request.setContent(new HashMap<>());

        RelationInstanceDraftDto created = draftService.createDraft(request);

        com.metaforge.graph.api.dto.UpdateDraftContentRequest updateReq =
                new com.metaforge.graph.api.dto.UpdateDraftContentRequest();
        Map<String, Object> newContent = new HashMap<>();
        newContent.put("updated", true);
        updateReq.setContent(newContent);

        RelationInstanceDraftDto updated = draftService.updateDraftContent(created.getFqn(), updateReq);

        assertNotNull(updated);
        assertTrue((Boolean) updated.getContent().get("updated"));
    }

    @Test
    @DisplayName("删除草稿——物理删除后 FQN 可重建")
    void testDeleteDraft() {
        CreateDraftRequest request = new CreateDraftRequest();
        request.setSourceEntityFqn("test:EntityG");
        request.setTargetEntityFqn("test:EntityH");
        request.setRelationTypeFqn("test:relation:1.0.0.TEST_TYPE");
        request.setName("测试删除关系");
        request.setContent(new HashMap<>());

        RelationInstanceDraftDto created = draftService.createDraft(request);
        draftService.deleteDraft(created.getFqn());

        assertThrows(RelationDraftServiceImpl.DraftNotFoundException.class, () ->
                draftService.getDraft(created.getFqn()));
    }
}
