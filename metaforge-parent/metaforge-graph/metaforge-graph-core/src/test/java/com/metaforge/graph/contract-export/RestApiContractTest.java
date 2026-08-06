package com.metaforge.graph.contract_export;

import com.metaforge.framework.test.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * REST API 契约测试——验证所有端点的 URL 可访问性。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Tag("integration")
@DisplayName("REST API 契约测试")
class RestApiContractTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/v1/graph/relations —— 条件列表查询端点可用")
    void testListRelationsEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/graph/relations").param("page", "1").param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/graph/versions —— 历史版本端点可用")
    void testVersionsEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/graph/versions/test"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/graph/topology/dependent-relations —— 拓扑端点可用")
    void testTopologyEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/graph/topology/dependent-relations")
                        .param("entityFqn", "test"))
                .andExpect(status().isOk());
    }
}
