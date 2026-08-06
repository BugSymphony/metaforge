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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 生效 REST API 集成测试。
 * 验证原子生效、下线、重新生效及前置校验。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Tag("integration")
@DisplayName("生效 REST API 集成测试")
class RelationActivationApiTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/v1/graph/relations/activate —— 草稿生效返回 200")
    void testActivate() throws Exception {
        CreateDraftRequest request = new CreateDraftRequest();
        request.setSourceEntityFqn("test:ActA");
        request.setTargetEntityFqn("test:ActB");
        request.setRelationTypeFqn("test:relation:1.0.0.TEST_TYPE");
        request.setName("生效测试关系");
        Map<String, Object> content = new HashMap<>();
        content.put("activate", true);
        request.setContent(content);

        String draftJson = objectMapper.writeValueAsString(request);
        String draftResult = mockMvc.perform(post("/api/v1/graph/drafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(draftJson))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String fqn = (String) objectMapper.readTree(draftResult)
                .get("data").get("fqn").asText();

        Map<String, String> activateBody = new HashMap<>();
        activateBody.put("fqn", fqn);

        mockMvc.perform(post("/api/v1/graph/relations/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(activateBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.fqn").value(fqn))
                .andExpect(jsonPath("$.data.currentVersion").isNumber());
    }

    @Test
    @DisplayName("POST /api/v1/graph/relations/check-deprecation —— 不存在的关系返回依赖校验通过")
    void testCheckDeprecation() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("fqn", "nonexistent-fqn");

        mockMvc.perform(post("/api/v1/graph/relations/check-deprecation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
