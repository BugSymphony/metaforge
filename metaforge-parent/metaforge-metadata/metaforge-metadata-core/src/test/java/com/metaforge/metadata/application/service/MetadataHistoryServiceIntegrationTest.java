package com.metaforge.metadata.application.service;

import com.metaforge.framework.test.BaseIntegrationTest;
import com.metaforge.common.dto.PageRequest;
import com.metaforge.common.dto.PageResult;
import com.metaforge.metadata.api.dto.response.EntityVersionDto;
import com.metaforge.metadata.api.service.MetadataHistoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Tag("integration")
@DisplayName("历史版本追溯集成测试")
class MetadataHistoryServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MetadataHistoryService historyService;

    @Test
    @DisplayName("查询全版本列表")
    void testListVersions() {
        PageResult<EntityVersionDto> result = historyService.listVersions("NonExistent_Test", new PageRequest(1, 20));
        assertNotNull(result);
        assertEquals(0, result.getTotal());
    }

    @Test
    @DisplayName("版本差异对比")
    void testCompareVersions() {
        com.metaforge.metadata.api.dto.request.DiffRequest request = new com.metaforge.metadata.api.dto.request.DiffRequest();
        request.setFqn("NonExistent_Version_Test");
        request.setVersionA(1);
        request.setVersionB(2);

        assertThrows(Exception.class, () -> historyService.compareVersions(request));
    }
}
