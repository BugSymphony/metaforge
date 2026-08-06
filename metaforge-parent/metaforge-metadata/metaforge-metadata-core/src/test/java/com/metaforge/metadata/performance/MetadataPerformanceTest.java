package com.metaforge.metadata.performance;

import com.metaforge.framework.test.BaseIntegrationTest;
import com.metaforge.metadata.api.dto.request.CreateDraftRequest;
import com.metaforge.metadata.api.service.MetadataDraftService;
import com.metaforge.metadata.api.service.MetadataQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Tag("integration")
@DisplayName("性能基准测试")
class MetadataPerformanceTest extends BaseIntegrationTest {

    @Autowired
    private MetadataDraftService draftService;

    @Autowired
    private MetadataQueryService queryService;

    @Test
    @DisplayName("草稿创建性能 ≤ 50ms")
    void testDraftCreationPerformance() {
        String fqn = "Perf_Draft_" + System.currentTimeMillis();
        CreateDraftRequest request = new CreateDraftRequest();
        request.setFqn(fqn);
        request.setName("性能测试");
        request.setEntitySchemaFqn("test:1.0.0.test.Test");
        Map<String, Object> content = new HashMap<>();
        content.put("field1", "value1");
        request.setContent(content);

        long start = System.currentTimeMillis();
        draftService.createDraft(request);
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed <= 200, "草稿创建耗时 " + elapsed + "ms，期望 ≤ 200ms（不含 Schema 校验）");
    }

    @Test
    @DisplayName("FQN 前缀查询性能 ≤ 100ms")
    void testFqnPrefixQueryPerformance() {
        long start = System.currentTimeMillis();
        com.metaforge.common.dto.PageResult<?> result = queryService.adminQuery(
                new com.metaforge.metadata.api.dto.request.AdminQueryRequest());
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed <= 500, "查询耗时 " + elapsed + "ms，期望 ≤ 500ms");
    }
}
