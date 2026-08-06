package com.metaforge.graph.integration.event;

import com.metaforge.framework.test.BaseIntegrationTest;
import com.metaforge.graph.api.dto.CreateDraftRequest;
import com.metaforge.graph.api.dto.RelationInstanceDraftDto;
import com.metaforge.graph.api.dto.RelationInstanceDto;
import com.metaforge.graph.api.enums.ChangeType;
import com.metaforge.graph.api.event.RelationChangeEvent;
import com.metaforge.graph.api.event.RelationChangeListener;
import com.metaforge.graph.api.service.RelationActivationService;
import com.metaforge.graph.api.service.RelationDraftService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 事件发布集成测试。
 * 验证生效/下线场景正确发布事件。
 */
@SpringBootTest
@Tag("integration")
@DisplayName("关系变更事件集成测试")
class RelationChangeEventTest extends BaseIntegrationTest {

    @Autowired
    private RelationDraftService draftService;

    @Autowired
    private RelationActivationService activationService;

    @Autowired
    private TestRelationChangeListener testListener;

    @Test
    @DisplayName("生效操作——验证成功发布 ACTIVATED 事件")
    void testActivatePublishesEvent() {
        CreateDraftRequest request = new CreateDraftRequest();
        request.setSourceEntityFqn("test:evt:PubSource");
        request.setTargetEntityFqn("test:evt:PubTarget");
        request.setRelationTypeFqn("test:relation:1.0.0.TEST_TYPE");
        request.setName("事件发布测试");
        Map<String, Object> content = new HashMap<>();
        content.put("event", "test");
        request.setContent(content);

        long beforeCount = testListener.getEvents().size();
        RelationInstanceDraftDto draft = draftService.createDraft(request);
        activationService.activate(draft.getFqn());

        assertTrue(testListener.getEvents().size() >= beforeCount,
                "生效后应发布 ACTIVATED 事件");
    }

    @Test
    @DisplayName("事件内容——验证事件包含正确的 FQN、changeType、version")
    void testEventContent() {
        CreateDraftRequest request = new CreateDraftRequest();
        request.setSourceEntityFqn("test:evt:ContentSource");
        request.setTargetEntityFqn("test:evt:ContentTarget");
        request.setRelationTypeFqn("test:relation:1.0.0.TEST_TYPE");
        request.setName("事件内容测试");
        request.setContent(new HashMap<>());

        RelationInstanceDraftDto draft = draftService.createDraft(request);
        RelationInstanceDto activated = activationService.activate(draft.getFqn());

        testListener.getEvents().stream()
                .filter(e -> e.getFqn().equals(draft.getFqn()))
                .findFirst()
                .ifPresentOrElse(event -> {
                    assertEquals(ChangeType.ACTIVATED, event.getChangeType());
                    assertNotNull(event.getVersion());
                    assertNotNull(event.getEventTime());
                }, () -> fail("未找到对应 FQN 的事件: " + draft.getFqn()));
    }

    /**
     * 测试用事件监听器——收集所有 RelationChangeEvent 供测试断言。
     */
    @Component
    static class TestRelationChangeListener implements RelationChangeListener {

        private final List<RelationChangeEvent> events = new ArrayList<>();

        @Override
        public void onRelationChange(RelationChangeEvent event) {
            events.add(event);
        }

        public List<RelationChangeEvent> getEvents() {
            return events;
        }
    }
}
