package com.metaforge.sample.controller;

import com.metaforge.framework.test.BaseIntegrationTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SampleController 集成测试，验证 REST 端点与响应结构。
 * 需要 Docker 环境运行 TestContainers，默认构建跳过，通过 `integration-test` profile 启用。
 */
@Tag("integration")
class SampleControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testHelloEndpoint() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/sample/hello", Map.class);
        assertEquals(200, response.getStatusCodeValue());
        Map body = response.getBody();
        assertNotNull(body);
        assertEquals(200, body.get("code"));
        assertEquals("success", body.get("message"));
        assertNotNull(body.get("traceId"));
    }
}
