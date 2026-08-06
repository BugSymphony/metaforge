package com.metaforge.metadata.application.service;

import com.metaforge.framework.test.BaseIntegrationTest;
import com.metaforge.common.dto.PageRequest;
import com.metaforge.common.dto.PageResult;
import com.metaforge.metadata.api.dto.request.MetadataQueryRequest;
import com.metaforge.metadata.api.dto.response.MetadataEntityDto;
import com.metaforge.metadata.api.service.MetadataQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Tag("integration")
@DisplayName("多维度查询检索集成测试")
class MetadataQueryServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MetadataQueryService queryService;

    @Test
    @DisplayName("FQN 前缀查询返回分页结果")
    void testListByFqnPrefixes() {
        MetadataQueryRequest request = new MetadataQueryRequest();
        request.setFqnPrefixes(List.of("Test_"));
        request.setPageRequest(new PageRequest(1, 20));

        PageResult<MetadataEntityDto> result = queryService.listByFqnPrefixes(request);

        assertNotNull(result);
        assertTrue(result.getTotal() >= 0);
    }

    @Test
    @DisplayName("按元模型类型查询")
    void testListByEntitySchema() {
        MetadataQueryRequest request = new MetadataQueryRequest();
        request.setEntitySchemaFqn("test:1.0.0.test.Test");
        request.setPageRequest(new PageRequest(1, 20));

        PageResult<MetadataEntityDto> result = queryService.listByEntitySchema(request);

        assertNotNull(result);
    }

    @Test
    @DisplayName("管理员全状态查询")
    void testAdminQuery() {
        com.metaforge.metadata.api.dto.request.AdminQueryRequest request = new com.metaforge.metadata.api.dto.request.AdminQueryRequest();
        request.setPageRequest(new PageRequest(1, 50));

        PageResult<MetadataEntityDto> result = queryService.adminQuery(request);

        assertNotNull(result);
        assertTrue(result.getTotal() >= 0);
    }
}
