package com.metaforge.graph.integration.rest;

import com.metaforge.framework.test.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 拓扑查询 API 集成测试。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Tag("integration")
@DisplayName("拓扑查询 API 集成测试")
class RelationTopologyApiTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/v1/graph/topology/dependent-relations —— 查询依赖关系")
    void testGetDependentRelations() throws Exception {
        mockMvc.perform(get("/api/v1/graph/topology/dependent-relations")
                        .param("entityFqn", "test:Topo:Entity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /api/v1/graph/topology/relation-count —— 查询关系计数")
    void testGetRelationCount() throws Exception {
        mockMvc.perform(get("/api/v1/graph/topology/relation-count")
                        .param("entityFqn", "test:Topo:Entity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.outboundCount").isNumber())
                .andExpect(jsonPath("$.data.inboundCount").isNumber());
    }
}
