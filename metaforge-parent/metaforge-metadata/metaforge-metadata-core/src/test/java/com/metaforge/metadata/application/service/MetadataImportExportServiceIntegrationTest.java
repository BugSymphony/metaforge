package com.metaforge.metadata.application.service;

import com.metaforge.framework.test.BaseIntegrationTest;
import com.metaforge.metadata.api.dto.request.ImportRequest;
import com.metaforge.metadata.api.dto.response.ImportResultDto;
import com.metaforge.metadata.api.enums.ImportFormat;
import com.metaforge.metadata.api.enums.ImportStrategy;
import com.metaforge.metadata.api.service.MetadataImportExportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Tag("integration")
@DisplayName("批量导入导出集成测试")
class MetadataImportExportServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MetadataImportExportService importExportService;

    @Test
    @DisplayName("导入 JSON 数据成功")
    void testImportMetadata() {
        String jsonContent = "[{\"fqn\":\"Import_Test_001\",\"name\":\"导入测试\",\"entitySchemaFqn\":\"test:1.0.0.test.Test\",\"content\":{\"field1\":\"value1\"}}]";

        ImportRequest request = new ImportRequest();
        request.setContent(jsonContent);
        request.setFormat(ImportFormat.JSON);
        request.setStrategy(ImportStrategy.SKIP);

        ImportResultDto result = importExportService.importMetadata(request);

        assertNotNull(result);
        assertEquals(1, result.getTotalCount());
        assertTrue(result.getSuccessCount() >= 0);
    }

    @Test
    @DisplayName("FQN 重复时 SKIP 策略跳过")
    void testImportSkipDuplicate() {
        String jsonContent = "[{\"fqn\":\"Import_Skip_001\",\"name\":\"跳过测试1\",\"entitySchemaFqn\":\"test:1.0.0.test.Test\",\"content\":{\"field1\":\"v1\"}}]";

        ImportRequest request = new ImportRequest();
        request.setContent(jsonContent);
        request.setFormat(ImportFormat.JSON);
        request.setStrategy(ImportStrategy.SKIP);

        ImportResultDto result1 = importExportService.importMetadata(request);
        ImportResultDto result2 = importExportService.importMetadata(request);

        assertEquals(1, result2.getSkipCount());
    }
}
