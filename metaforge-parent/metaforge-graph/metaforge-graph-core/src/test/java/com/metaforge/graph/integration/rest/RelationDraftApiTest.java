package com.metaforge.graph.integration.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.metaforge.framework.test.BaseIntegrationTest;
import com.metaforge.graph.api.dto.CreateDraftRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 草稿管理 REST API 集成测试。
 * 验证 HTTP 状态码、响应格式与错误码。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Tag("integration")
@DisplayName("草稿管理 REST API 集成测试")
class RelationDraftApiTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/v1/graph/drafts —— 创建草稿返回 200")
    void testCreateDraft() throws Exception {
        CreateDraftRequest request = new CreateDraftRequest();
        request.setSourceEntityFqn("test:ApiA");
        request.setTargetEntityFqn("test:ApiB");
        request.setRelationTypeFqn("test:relation:1.0.0.TEST_TYPE");
        request.setName("API测试关系");
        Map<String, Object> content = new HashMap<>();
        content.put("api", "test");
        request.setContent(content);

        mockMvc.perform(post("/api/v1/graph/drafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.fqn").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/v1/graph/drafts/{fqn} —— 草稿不存在返回 404 业务错误码")
    void testGetDraftNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/graph/drafts/non-existent-fqn"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/graph/drafts/{fqn} —— 删除不存在的草稿返回 404")
    void testDeleteDraftNotFound() throws Exception {
        mockMvc.perform(delete("/api/v1/graph/drafts/non-existent-fqn"))
                .andExpect(status().isNotFound());
    }
}
