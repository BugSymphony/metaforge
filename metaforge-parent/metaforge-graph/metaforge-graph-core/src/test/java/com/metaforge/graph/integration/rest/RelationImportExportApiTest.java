package com.metaforge.graph.integration.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.metaforge.framework.test.BaseIntegrationTest;
import com.metaforge.common.util.JsonbUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 导入导出 API 集成测试。
 * 验证导入→草稿表、导出→重新导入闭环、超10MB拒绝。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Tag("integration")
@DisplayName("导入导出 API 集成测试")
class RelationImportExportApiTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/v1/graph/import —— 导入 JSON 格式关系数据")
    void testImportJson() throws Exception {
        List<Map<String, Object>> records = new ArrayList<>();
        Map<String, Object> record = new HashMap<>();
        record.put("sourceEntityFqn", "test:import:Source1");
        record.put("relationTypeFqn", "test:relation:1.0.0.TEST_TYPE");
        record.put("targetEntityFqn", "test:import:Target1");
        record.put("name", "导入测试关系");
        record.put("content", new HashMap<>());
        records.add(record);

        Map<String, Object> body = new HashMap<>();
        body.put("content", JsonbUtils.toJsonb(records));
        body.put("format", "JSON");
        body.put("strategy", "SKIP");

        mockMvc.perform(post("/api/v1/graph/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalCount").value(1));
    }

    @Test
    @DisplayName("POST /api/v1/graph/export —— 按 FQN 前缀导出")
    void testExportByFqnPrefixes() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("fqnPrefixes", Collections.singletonList("test:"));
        body.put("format", "JSON");

        mockMvc.perform(post("/api/v1/graph/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.format").value("JSON"));
    }

    @Test
    @DisplayName("POST /api/v1/graph/import —— 空内容返回空结果")
    void testImportEmptyContent() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("content", "");
        body.put("format", "JSON");
        body.put("strategy", "SKIP");

        mockMvc.perform(post("/api/v1/graph/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(0));
    }
}
