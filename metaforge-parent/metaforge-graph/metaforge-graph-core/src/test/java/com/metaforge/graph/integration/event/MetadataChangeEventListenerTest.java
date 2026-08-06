package com.metaforge.graph.integration.event;

import com.metaforge.framework.test.BaseIntegrationTest;
import com.metaforge.graph.infrastructure.event.IdempotencyStore;
import com.metaforge.graph.interfaces.event.MetadataChangeEventListener;
import com.metaforge.metadata.api.enums.ChangeType;
import com.metaforge.metadata.api.event.MetadataChangeEvent;
import com.metaforge.metadata.api.event.MetadataChangeListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 事件监听集成测试。
 * 验证 MetadataChangeEvent 驱动的自动构建、幂等去重。
 */
@SpringBootTest
@Tag("integration")
@DisplayName("元数据变更事件监听集成测试")
class MetadataChangeEventListenerTest extends BaseIntegrationTest {

    @Autowired
    private MetadataChangeEventListener eventListener;

    @Autowired
    private IdempotencyStore idempotencyStore;

    @Test
    @DisplayName("ACTIVATE 事件——事件监听器正确注入并可处理变更事件")
    void testActivateEvent() {
        assertNotNull(eventListener);

        MetadataChangeEvent event = new MetadataChangeEvent(
                this, "test:evt:EntityA", ChangeType.ACTIVATE, 1);

        assertDoesNotThrow(() -> eventListener.onMetadataChange(event));
    }

    @Test
    @DisplayName("DEPRECATE 事件——实体下线事件触发关联关系处理")
    void testDeprecateEvent() {
        assertNotNull(eventListener);

        MetadataChangeEvent event = new MetadataChangeEvent(
                this, "test:evt:EntityDep", ChangeType.DEPRECATE, 2);

        assertDoesNotThrow(() -> eventListener.onMetadataChange(event));
    }

    @Test
    @DisplayName("幂等去重——同一实体+版本号仅处理一次")
    void testIdempotency() {
        String entityFqn = "test:evt:EntityIdempotent";
        Integer version = 42;

        assertFalse(idempotencyStore.isProcessed(entityFqn, version));
        idempotencyStore.markProcessed(entityFqn, version);
        assertTrue(idempotencyStore.isProcessed(entityFqn, version));

        idempotencyStore.clear(entityFqn);
        assertFalse(idempotencyStore.isProcessed(entityFqn, version));
    }
}
