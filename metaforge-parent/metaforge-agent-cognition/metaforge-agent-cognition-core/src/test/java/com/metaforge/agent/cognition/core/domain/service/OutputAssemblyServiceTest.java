package com.metaforge.agent.cognition.core.domain.service;

import com.metaforge.agent.cognition.api.dto.request.CognitionRequest;
import com.metaforge.agent.cognition.api.dto.request.Scope;
import com.metaforge.agent.cognition.api.enums.AgentArchetype;
import com.metaforge.agent.cognition.api.enums.CognitionDepth;
import com.metaforge.agent.cognition.api.enums.DimensionCategory;
import com.metaforge.agent.cognition.api.spi.CognitionResult;
import com.metaforge.agent.cognition.api.dto.response.CognitionResponse;
import com.metaforge.agent.cognition.core.domain.model.aggregate.CognitionQuery;
import com.metaforge.agent.cognition.core.domain.model.entity.OperatorDefinition;
import com.metaforge.agent.cognition.core.domain.model.entity.TemplateDefinition;
import com.metaforge.agent.cognition.core.domain.model.valueobject.OperatorId;
import com.metaforge.agent.cognition.core.domain.model.valueobject.ScopeBehavior;
import com.metaforge.agent.cognition.core.domain.model.valueobject.TemplateId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("OutputAssemblyService updated_scope 聚合测试")
class OutputAssemblyServiceTest {

    OutputAssemblyService service;
    ContextMetaService contextMetaService;

    @BeforeEach
    void setUp() {
        contextMetaService = mock(ContextMetaService.class);
        service = new OutputAssemblyService(contextMetaService);
    }

    private TemplateDefinition template(boolean producesUpdatedScope) {
        TemplateDefinition def = new TemplateDefinition();
        def.setTemplateId("TEST");
        ScopeBehavior sb = new ScopeBehavior();
        sb.setAcceptsScope(true);
        sb.setScopeRequired(false);
        sb.setProducesUpdatedScope(producesUpdatedScope);
        sb.setScopeFields(List.of("bundles", "packages"));
        def.setScopeBehavior(sb);
        OperatorDefinition op = new OperatorDefinition();
        op.setOperatorId("test.discovery");
        op.setRequired(true);
        def.setOperators(List.of(op));
        return def;
    }

    private CognitionQuery query(boolean producesUpdatedScope, CognitionResult result) {
        CognitionRequest req = new CognitionRequest(
                Scope.EMPTY, Map.of(), "JSON", CognitionDepth.L3, AgentArchetype.EXECUTION, 8000);
        CognitionQuery q = new CognitionQuery(new TemplateId("TEST"), req);
        q.loadTemplate(template(producesUpdatedScope));
        q.addExecutionResult(new OperatorId("test.discovery"), result);
        return q;
    }

    private CognitionResult resultWithUpdatedScope() {
        Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("bundles", List.of("order:1.0.0"));
        data.put("updated_scope", Map.of("bundles", List.of("order:1.0.0")));
        return CognitionResult.success("test.discovery", DimensionCategory.ONTOLOGICAL, data);
    }

    private CognitionResult plainResult() {
        Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("bundles", List.of("order:1.0.0"));
        return CognitionResult.success("test.discovery", DimensionCategory.ONTOLOGICAL, data);
    }

    @Test
    @DisplayName("producesUpdatedScope=true 时聚合算子的 updated_scope 到响应顶层")
    void shouldAggregateUpdatedScopeWhenEnabled() {
        CognitionQuery q = query(true, resultWithUpdatedScope());

        CognitionResponse response = service.assemble(q);

        assertThat(response.updatedScope())
                .containsEntry("bundles", List.of("order:1.0.0"));
    }

    @Test
    @DisplayName("producesUpdatedScope=false 时 updated_scope 为 null（响应不出现该字段）")
    void shouldNotAggregateWhenDisabled() {
        CognitionQuery q = query(false, resultWithUpdatedScope());

        CognitionResponse response = service.assemble(q);

        assertThat(response.updatedScope()).isNull();
    }

    @Test
    @DisplayName("算子无 updated_scope 时聚合结果为空")
    void shouldReturnEmptyWhenNoUpdatedScopeInResult() {
        CognitionQuery q = query(true, plainResult());

        CognitionResponse response = service.assemble(q);

        assertThat(response.updatedScope()).isEmpty();
    }
}
