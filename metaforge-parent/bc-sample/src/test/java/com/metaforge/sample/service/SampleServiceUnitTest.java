package com.metaforge.sample.service;

import com.metaforge.framework.test.BaseUnitTest;
import com.metaforge.sample.model.SampleEntity;
import com.metaforge.sample.repository.SampleRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.cache.CacheManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

/**
 * SampleService 单元测试，验证业务逻辑正确性。
 */
class SampleServiceUnitTest extends BaseUnitTest {

    @Mock
    private SampleRepository sampleRepository;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private com.metaforge.framework.cache.CacheTemplate cacheTemplate;

    @InjectMocks
    private SampleService sampleService;

    @Test
    void testHello() {
        String result = sampleService.hello();
        assertEquals("Hello from MetaForge bc-sample!", result);
    }
}
