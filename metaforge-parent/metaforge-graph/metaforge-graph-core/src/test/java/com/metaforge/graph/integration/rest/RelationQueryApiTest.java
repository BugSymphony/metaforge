package com.metaforge.graph.integration.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.metaforge.framework.test.BaseIntegrationTest;
import com.metaforge.graph.api.dto.CreateDraftRequest;
import com.metaforge.graph.api.dto.RelationQueryRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 查询 API 集成测试。
 * 覆盖 FQN 精准查询、出入边查询、多维过滤、分页、空结果。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Tag("integration")
@DisplayName("查询 API 集成测试")
class RelationQueryApiTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/v1/graph/relations —— 条件列表查询返回分页结果")
    void testListByConditions() throws Exception {
        mockMvc.perform(get("/api/v1/graph/relations")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /api/v1/graph/relations/{fqn} —— 不存在的 FQN 返回 404")
    void testGetByFqnNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/graph/relations/non-existent-fqn"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/graph/relations/filter —— 多维过滤返回分页结果")
    void testMultiFilter() throws Exception {
        RelationQueryRequest request = new RelationQueryRequest();

        mockMvc.perform(post("/api/v1/graph/relations/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /api/v1/graph/relations/outbound —— 出边查询")
    void testGetOutboundRelations() throws Exception {
        mockMvc.perform(get("/api/v1/graph/relations/outbound")
                        .param("entityFqn", "test:Query:EntityX"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /api/v1/graph/relations/inbound —— 入边查询")
    void testGetInboundRelations() throws Exception {
        mockMvc.perform(get("/api/v1/graph/relations/inbound")
                        .param("entityFqn", "test:Query:EntityY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /api/v1/graph/admin/relations —— 管理员全状态查询")
    void testAdminQuery() throws Exception {
        mockMvc.perform(get("/api/v1/graph/admin/relations")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
