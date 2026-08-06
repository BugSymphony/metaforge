package com.metaforge.metadata.application.service;

import com.metaforge.framework.test.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.metaforge.metadata.api.dto.request.CreateDraftRequest;
import com.metaforge.metadata.api.dto.response.MetadataEntityDraftDto;
import com.metaforge.metadata.api.service.MetadataDraftService;
import com.metaforge.metadata.domain.exception.FqnConflictException;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Tag("integration")
@DisplayName("草稿管理集成测试")
class MetadataDraftServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MetadataDraftService draftService;

    @Test
    @DisplayName("创建草稿成功")
    void testCreateDraft() {
        CreateDraftRequest request = new CreateDraftRequest();
        request.setFqn("Test_Entity_001");
        request.setName("测试实体");
        request.setEntitySchemaFqn("test:1.0.0.test.Test");
        Map<String, Object> content = new HashMap<>();
        content.put("field1", "value1");
        request.setContent(content);

        MetadataEntityDraftDto draft = draftService.createDraft(request);

        assertNotNull(draft);
        assertEquals("Test_Entity_001", draft.getFqn());
        assertEquals("测试实体", draft.getName());
    }

    @Test
    @DisplayName("FQN 重复创建草稿应抛出冲突异常")
    void testCreateDraftFqnConflict() {
        CreateDraftRequest request = new CreateDraftRequest();
        request.setFqn("Test_Conflict_001");
        request.setName("冲突测试");
        request.setEntitySchemaFqn("test:1.0.0.test.Test");
        Map<String, Object> content = new HashMap<>();
        content.put("field1", "value1");
        request.setContent(content);

        draftService.createDraft(request);

        assertThrows(FqnConflictException.class, () -> draftService.createDraft(request));
    }

    @Test
    @DisplayName("查询草稿成功")
    void testGetDraft() {
        CreateDraftRequest request = new CreateDraftRequest();
        request.setFqn("Test_Get_001");
        request.setName("查询测试");
        request.setEntitySchemaFqn("test:1.0.0.test.Test");
        Map<String, Object> content = new HashMap<>();
        content.put("field1", "value1");
        request.setContent(content);
        draftService.createDraft(request);

        MetadataEntityDraftDto draft = draftService.getDraft("Test_Get_001");
        assertNotNull(draft);
        assertEquals("Test_Get_001", draft.getFqn());
    }

    @Test
    @DisplayName("删除草稿成功")
    void testDeleteDraft() {
        CreateDraftRequest request = new CreateDraftRequest();
        request.setFqn("Test_Delete_001");
        request.setName("删除测试");
        request.setEntitySchemaFqn("test:1.0.0.test.Test");
        Map<String, Object> content = new HashMap<>();
        content.put("field1", "value1");
        request.setContent(content);
        draftService.createDraft(request);

        draftService.deleteDraft("Test_Delete_001");

        assertThrows(Exception.class, () -> draftService.getDraft("Test_Delete_001"));
    }
}
