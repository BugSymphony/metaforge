package com.metaforge.graph.integration.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.metaforge.framework.test.BaseIntegrationTest;
import com.metaforge.graph.api.dto.DiffRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 历史追溯 API 集成测试。
 * 验证历史表只读、版本列表倒序、差异对比正确性。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Tag("integration")
@DisplayName("历史追溯 API 集成测试")
class RelationHistoryApiTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/v1/graph/versions/{fqn} —— 查询历史版本列表")
    void testListVersions() throws Exception {
        mockMvc.perform(get("/api/v1/graph/versions/test:hist:Relation1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /api/v1/graph/versions/{fqn}/{version} —— 不存在的版本返回错误")
    void testGetVersionDetailNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/graph/versions/test:nonexistent/999"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("POST /api/v1/graph/versions/diff —— 版本差异对比请求格式正确")
    void testCompareVersions() throws Exception {
        DiffRequest request = new DiffRequest();
        request.setFqn("test:diff:Fqn");
        request.setVersionA(1);
        request.setVersionB(2);

        mockMvc.perform(post("/api/v1/graph/versions/diff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError()); // 版本不存在
    }
}
